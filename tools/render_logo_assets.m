#import <CoreGraphics/CoreGraphics.h>
#import <Foundation/Foundation.h>
#import <ImageIO/ImageIO.h>

typedef struct {
    int x;
    int y;
    int width;
    int height;
} LogoRectangle;

static bool pointInsideRoundedSquare(double x, double y, double side, double radius) {
    if ((x >= radius && x <= side - radius) || (y >= radius && y <= side - radius)) {
        return x >= 0 && x <= side && y >= 0 && y <= side;
    }

    double centerX = x < radius ? radius : side - radius;
    double centerY = y < radius ? radius : side - radius;
    double deltaX = x - centerX;
    double deltaY = y - centerY;
    return deltaX * deltaX + deltaY * deltaY <= radius * radius;
}

static NSString *halfPoint(int value) {
    return value % 2 == 0
        ? [NSString stringWithFormat:@"%d", value / 2]
        : [NSString stringWithFormat:@"%d.5", value / 2];
}

static NSString *traceForeground(CGImageRef source) {
    const int sampleSize = 80;
    const size_t bytesPerRow = sampleSize * 4;
    uint8_t *pixels = calloc(sampleSize * bytesPerRow, 1);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(
        pixels,
        sampleSize,
        sampleSize,
        8,
        bytesPerRow,
        colorSpace,
        kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big
    );
    CGColorSpaceRelease(colorSpace);
    if (context == NULL) {
        free(pixels);
        return nil;
    }

    CGContextSetInterpolationQuality(context, kCGInterpolationHigh);
    CGContextDrawImage(context, CGRectMake(0, 0, sampleSize, sampleSize), source);

    LogoRectangle rectangles[2048];
    int rectangleCount = 0;
    const double sampleRadius = sampleSize * 0.14;

    for (int y = 0; y < sampleSize; y++) {
        int x = 0;
        while (x < sampleSize) {
            size_t offset = y * bytesPerRow + x * 4;
            int red = pixels[offset];
            int green = pixels[offset + 1];
            int blue = pixels[offset + 2];
            int maximum = MAX(red, MAX(green, blue));
            int minimum = MIN(red, MIN(green, blue));
            bool insideIcon = pointInsideRoundedSquare(
                x + 0.5,
                y + 0.5,
                sampleSize,
                sampleRadius
            );
            bool insideMarkRegion = x >= 8 && x < 72 && y >= 8 && y < 72;
            bool foreground = insideIcon && insideMarkRegion &&
                minimum >= 232 && maximum - minimum <= 22;

            if (!foreground) {
                x++;
                continue;
            }

            int runStart = x;
            while (x < sampleSize) {
                offset = y * bytesPerRow + x * 4;
                red = pixels[offset];
                green = pixels[offset + 1];
                blue = pixels[offset + 2];
                maximum = MAX(red, MAX(green, blue));
                minimum = MIN(red, MIN(green, blue));
                insideIcon = pointInsideRoundedSquare(
                    x + 0.5,
                    y + 0.5,
                    sampleSize,
                    sampleRadius
                );
                insideMarkRegion = x >= 8 && x < 72 && y >= 8 && y < 72;
                foreground = insideIcon && insideMarkRegion &&
                    minimum >= 232 && maximum - minimum <= 22;
                if (!foreground) break;
                x++;
            }

            int runWidth = x - runStart;
            int matchingRectangle = -1;
            for (int index = rectangleCount - 1; index >= 0; index--) {
                LogoRectangle candidate = rectangles[index];
                if (candidate.y + candidate.height < y) break;
                if (
                    candidate.y + candidate.height == y &&
                    candidate.x == runStart &&
                    candidate.width == runWidth
                ) {
                    matchingRectangle = index;
                    break;
                }
            }

            if (matchingRectangle >= 0) {
                rectangles[matchingRectangle].height++;
            } else if (rectangleCount < 2048) {
                rectangles[rectangleCount++] = (LogoRectangle){runStart, y, runWidth, 1};
            } else {
                fprintf(stderr, "Logo is too complex to vectorize.\n");
                CGContextRelease(context);
                free(pixels);
                return nil;
            }
        }
    }

    NSMutableString *path = [NSMutableString string];
    for (int index = 0; index < rectangleCount; index++) {
        LogoRectangle rectangle = rectangles[index];
        [path appendFormat:
            @"M%@ %@h%@v%@h-%@z",
            halfPoint(rectangle.x),
            halfPoint(rectangle.y),
            halfPoint(rectangle.width),
            halfPoint(rectangle.height),
            halfPoint(rectangle.width)
        ];
    }

    CGContextRelease(context);
    free(pixels);
    return path;
}

static NSString *logoBody(NSString *foregroundPath) {
    return [NSString stringWithFormat:
        @"  <defs><linearGradient id=\"g\" gradientUnits=\"userSpaceOnUse\" x1=\"0\" y1=\"0\" x2=\"40\" y2=\"40\"><stop stop-color=\"#4FADE3\"/><stop offset=\".5\" stop-color=\"#7187E2\"/><stop offset=\"1\" stop-color=\"#9668D9\"/></linearGradient></defs>\n"
         "  <rect width=\"40\" height=\"40\" rx=\"5.6\" fill=\"url(#g)\"/>\n"
         "  <path fill=\"#FFFFFF\" d=\"%@\"/>\n",
        foregroundPath
    ];
}

static NSString *pluginLogo(NSString *foregroundPath) {
    return [NSString stringWithFormat:
        @"<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"40\" height=\"40\" viewBox=\"0 0 40 40\">\n"
         "%@"
         "</svg>\n",
        logoBody(foregroundPath)
    ];
}

static NSString *actionLogo(NSString *foregroundPath) {
    NSString *body = logoBody(foregroundPath);
    NSString *indentedBody = [body stringByReplacingOccurrencesOfString:@"\n" withString:@"\n  "];
    return [NSString stringWithFormat:
        @"<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 40 40\">\n"
         "  <g transform=\"translate(2.5 2.5) scale(.875)\">\n"
         "  %@"
         "</g>\n"
         "</svg>\n",
        indentedBody
    ];
}

static bool writeText(NSString *text, const char *path) {
    NSError *error = nil;
    BOOL wroteFile = [text writeToFile:@(path)
                            atomically:YES
                              encoding:NSUTF8StringEncoding
                                 error:&error];
    if (!wroteFile) fprintf(stderr, "%s\n", error.localizedDescription.UTF8String);
    return wroteFile;
}

int main(int argc, const char *argv[]) {
    @autoreleasepool {
        if (argc != 4) {
            fprintf(
                stderr,
                "Usage: render_logo_assets <source.png> <action.svg> <pluginIcon.svg>\n"
            );
            return 2;
        }

        NSURL *sourceURL = [NSURL fileURLWithPath:@(argv[1])];
        CGImageSourceRef imageSource = CGImageSourceCreateWithURL((__bridge CFURLRef)sourceURL, NULL);
        CGImageRef source = imageSource == NULL ? NULL : CGImageSourceCreateImageAtIndex(imageSource, 0, NULL);
        if (source == NULL) {
            fprintf(stderr, "Unable to read source image.\n");
            if (imageSource != NULL) CFRelease(imageSource);
            return 1;
        }

        if (CGImageGetWidth(source) != CGImageGetHeight(source)) {
            fprintf(stderr, "Source image must be square.\n");
            CGImageRelease(source);
            CFRelease(imageSource);
            return 1;
        }

        NSString *foregroundPath = traceForeground(source);
        bool wroteAssets = foregroundPath != nil &&
            writeText(actionLogo(foregroundPath), argv[2]) &&
            writeText(pluginLogo(foregroundPath), argv[3]);

        CGImageRelease(source);
        CFRelease(imageSource);

        if (!wroteAssets) {
            fprintf(stderr, "Unable to render logo assets.\n");
            return 1;
        }
    }

    return 0;
}
