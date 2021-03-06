# JSON schemas
## Name convention
Each JSON file has name with following structure:
    `{scope}`.`{symbolic id}`.`{direction}`.`{kind}`.`[variant]`.json
<li>`{scope}`
  
  > __frontend__ &ndash; means that it is browser-server message  
  > __backend__ &ndash; means that it is inner serverside message
  
<li>`{symbolic id}` &ndash; message unique id or name&nbsp;&ndash; can contain dots inside as word delimiter
<li>`{direction}` &ndash; __request__ or __response__
<li>`{kind}`
  
  > __schema__ &ndash; it is JSON schema (see: [json-schema.org](http://json-schema.org))  
  > __sample__ &ndash; it is sample of message in accordance with its schema
  
<li>`{variant}` &ndash; variant name or number of sample (optional) ;&ndash; has the meaning for samples only.

## Frontend messages
`{symbolic id}` and `{direction}` parts only.
- `auth.response`  &ndash; authorization result which is returned on successful authorization. If authorization fails `401` status code is returned.
- `recognized.response`  &ndash; user info if user session was acknowledged. If session is not acknowledged `401` status code is returned.
- `byid.request` &ndash; get songs by exact song id. Ids array is allowed.
- `keywords.request` &ndash; search by keywords.
- `similar.request` &ndash; search songs by rhythmic pattern, keywords and tags. Message `songs.response` will be returned.
- `songs.response` &ndash; songs list. Each song with all attributes.
It is response for `byid.request`, `keywords.request`, `similar.request`.
- `tags.request.get.url` &ndash; this file contains URL for GET request for all tags list.
- `suggest.title.request` &ndash; suggest titles by first letters.
- `suggest.title.response` &ndash; suggested titles.
- `presyllables.request` &ndash; song's text for syllables processing. Message `syllables.response` will be returned.
- `syllables.response` &ndash; song with syllables array for each text row.
- `feedback.request` &ndash; any JSON with feedback form data. It is saved "as is"" in DB. Server always returns `200` status code and void data.
- `video.id.request` &ndash; song id to find corresponded video.
- `video.id.response` &ndash; video id for song.
- `reqinvite.request` &ndash; e-mail of person who wants invite
- `reqinvite.response` &ndash; result of invite request

## Requests' paths
- `auth.request` &rArr; `GET` `songs/env/auth`. Parameters: `email`.
- `recognize.request` &rArr; `GET` `songs/env/recognize`. Parameters: `session`. `recognized.response` is expected.
- `byid.request` &rArr; `POST` `songs/search/byid`.
- `keywords.request` &rArr; `POST` `songs/search/keywords`.
- `similar.request` &rArr; `POST` `songs/search/similar`.
- `tags.request.get` &rArr; `GET` `songs/env/tags`. Parameters: `lang`
- `suggest.title` &rArr; `POST` `songs/search/suggest_title`.
- `presyllables.request` &rArr; `POST` `songs/search/presyllables`.
- `feedback.request` &rArr; `POST` `songs/env/feedback`.
- `video.id.request` &rArr; `POST` `songs/env/video_id`. `video.id.response` is expected on success or `404` status code on fail.
- `reqinvite.request` &rArr; `POST` `songs/env/req_invite`. `reqinvite.response` will be returned with success code or error code and message.

Schemas and samples can be validated at: http://www.jsonschemavalidator.net
