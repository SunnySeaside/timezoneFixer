# Background
The EXIF standard does not specify the timezone of DateTimeOriginal, so it could be either local time or UTC. The only reliable EXIF tag for time taken is GPSTimeStamp, which is always in UTC. Unfortunately, GPSTimeStamp is only found on photos with GPS coordinations, which has caused problems after I imported some existing photos into my Android phone.
Specifically, photos with GPS coordinations have correct timestamps, but for those without GPS data, DateTimeTaken is interpreted as UTC rather than local time(which is used when taking new photos), leading to incorrect timestamps.

This app aims to fix this problem by overwriting DATE_TAKEN in the Android MediaStore for photos without GPS data, interpreting GPSTimeStamp as local time.

# TODO
- More beautiful GUI
- Support manually editing timestamps
- Some method to allow different timezones for different photos
- Support manually specifying timezones
