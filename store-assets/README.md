# Store assets

- `medikin-play-store-icon-512.png`: final 512×512 32-bit RGBA MediKin Play listing icon.
- `play-store-icon-512-v2.png` and `play-store-icon-512.png`: legacy drafts retained only as generation history; do not upload.
- `feature-graphic-1024x500.png`: final 1024×500 Play feature graphic.
- `feature-graphic-source.png`: high-resolution editable source generated for the feature graphic.

## Phone screenshots

`screenshots/full/` contains uncropped 1080×2424 captures from the emulator's native display:

- `01-today-medicines-1080x2424.png`
- `02-add-custom-time-1080x2424.png`
- `03-medicine-stock-refill-1080x2424.png`
- `04-family-missed-dose-1080x2424.png`

`screenshots/play/` contains the upload-ready 1212×2424 versions. These retain every pixel of the native screenshots at its original size and add only 66 pixels of matching background on each side to meet Google Play's 2:1 maximum aspect-ratio rule:

- `01-today-medicines-1212x2424.png`
- `02-add-custom-time-1212x2424.png`
- `03-medicine-stock-refill-1212x2424.png`
- `04-family-missed-dose-1212x2424.png`

The source visuals were generated with the built-in image generation tool. The MediKin icon prompt requested a full-bleed deep-evergreen square with a centered capsule cradled in two caring hands that subtly form a heart, using warm amber and white, with no text, watermark, or medical cross. The feature graphic prompt requested a warm flat family-care illustration with hands protecting a capsule, clock, and family in the app's evergreen/mint/marigold/coral palette, with no text or watermark. The final files were resized or padded to exact Play Console dimensions.
