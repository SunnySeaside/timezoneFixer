# Background
The EXIF standard does not specify the timezone of DateTimeOriginal, so it could be either local time or UTC. The only reliable EXIF tag for time taken is GPSTimeStamp, which is always in UTC. Unfortunately, GPSTimeStamp is only found on photos with GPS coordinations, which has caused problems after I imported some existing photos into my Android phone.
Specifically:
- Photos with GPS coordinations have correct timestamps, thanks to the GPSTimeStamp tag.
- For photos without GPS data, DateTimeOriginal is used as the time taken. However, it is interpreted as UTC rather than local time(as how it's used when taking new photos), leading to incorrect timestamps.

This app aims to fix this problem by overwriting DATE_TAKEN in the Android MediaStore for photos without GPS data, interpreting GPSTimeStamp as local time.

# TODO
- More beautiful & user-friendly GUI
- Support manually selecting which photos to fix
- Support manually editing timestamps of individual photos
- Some way to automatically assign different timezones to different photos
