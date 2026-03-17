# Spotify Clone - Domain Entities

Domain layer không được có bất kỳ Spring annotation nào. Đây là các plain Java objects với behavior.

## 1. Track
- **Fields:** id, title, duration, audioUrl, artistId, albumId, genre, playCount, isExplicit
- **Business Rule:** duration phải > 0, title không được rỗng
- **Events:** TrackUploaded, TrackPlayed, TrackDeleted

## 2. User
- **Fields:** id, email, password, displayName, avatarUrl, createdAt
- **Business Rule:** email phải unique
- **Events:** UserRegistered, UserProfileUpdated

## 3. Playlist
- **Fields:** id, name, ownerId, tracks, isPublic, collaboratorIds
- **Business Rule:** owner luôn có quyền edit, collaborator chỉ add/remove tracks
- **Events:** PlaylistCreated, TrackAddedToPlaylist

## 4. Artist
- **Fields:** id, userId, stageName, bio, monthlyListeners, albums
- **Business Rule:** stageName không được trùng
- **Events:** ArtistVerified

## 5. Album
- **Fields:** id, title, artistId, tracks, releaseDate, coverArtUrl
- **Business Rule:** releaseDate không được là tương lai khi publish
- **Events:** AlbumPublished
