# Bundled album artwork

The seven catalog covers are stored in [`drawable-nodpi`](../android/app/src/main/res/drawable-nodpi/) and included in the Android APK. Home, playlist and favorite views select the bundled image by album ID, including favorites saved with the earlier remote cover URLs. These catalog covers do not require an image-host request at runtime.

Retrieved on 2026-11-05. Images were downloaded without editing from the sources below. The artwork belongs to its respective rights holders; it is not original MelodySync artwork.

| Album ID | Album | Local asset | Source page |
| --- | --- | --- | --- |
| 1 | Leessang — Hexagonal | [album_hexagonal.jpg](../android/app/src/main/res/drawable-nodpi/album_hexagonal.jpg) | [Shazam / Apple artwork](https://www.shazam.com/song/339950183/cant-breakup-girl-cant-breakaway-boy-feat-jung-in) |
| 2 | Jay Chou — Still Fantasy | [album_still_fantasy.jpg](../android/app/src/main/res/drawable-nodpi/album_still_fantasy.jpg) | [Apple Music](https://music.apple.com/us/album/%E4%BE%9D%E7%84%B6%E8%8C%83%E7%89%B9%E8%A5%BF/1721453996) |
| 3 | Michael Jackson — Dangerous | [album_dangerous.jpg](../android/app/src/main/res/drawable-nodpi/album_dangerous.jpg) | [Michael Jackson official discography](https://www.michaeljackson.com/albums/dangerous/) |
| 4 | Stefanie Sun — Kite | [album_kite.jpg](../android/app/src/main/res/drawable-nodpi/album_kite.jpg) | [Apple Music](https://music.apple.com/tw/album/%E9%A2%A8%E7%AE%8F/297994281) |
| 5 | JJ Lin — No. 89757 | [album_no_89757.jpg](../android/app/src/main/res/drawable-nodpi/album_no_89757.jpg) | [Spotify](https://open.spotify.com/album/5OetT1AzRblq9NstfYM4Xv) |
| 6 | (G)I-dle — Nxde (Steve Aoki Remix) | [album_nxde_remix.jpg](../android/app/src/main/res/drawable-nodpi/album_nxde_remix.jpg) | [Apple Music](https://music.apple.com/cn/album/nxde-steve-aoki-remix-single/1717114048) |
| 7 | BLACKPINK — BLACKPINK IN YOUR AREA | [album_blackpink_in_your_area.jpg](../android/app/src/main/res/drawable-nodpi/album_blackpink_in_your_area.jpg) | [Official YGEX discography](https://ygex.jp/blackpink/discography/detail.php?id=1016135) |

Direct image URLs, dimensions, byte counts and SHA-256 hashes are recorded in [artwork-sources.json](artwork-sources.json). Unknown future album IDs continue to use their supplied cover URLs, with the local placeholder on failure.
