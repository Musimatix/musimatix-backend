# JSON schemas
## Name convention
Each JSON file has name with following structure:
    `{scope}`.`{symbolic id}`.`{direction}`.`{kind}`.`[variant]`.json
- `{scope}`
 
  > __frontend__ &ndash; means that it is browser-server message  
  > __backend__ &ndash; means that it is inner serverside message

- `{symbolic id}` &ndash; message unique id or name&nbsp;&ndash; can contain dots inside as word delimiter
- `{direction}` &ndash; __request__ or __response__
- `{kind}`

  > __schema__ &ndash; it is JSON schema (see: [json-schema.org](http://json-schema.org))  
  > __sample__ &ndash; it is sample of message in accordance with its schema

- `{variant}` &ndash; variant name or number of sample (optional) ;&ndash; has the meaning for samples only.

## Frontend messages
`{symbolic id}` and `{direction}` parts only.
- `auth.response`  &ndash; authorization result which is returned on successful authorization. If authorization fails `401` status code is returned.
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
- `feedback.request` &ndash; Any JSON with feedback form data. It is saved "as is"" in DB. Server always returns `200` status code and void data.

## Requests' paths
- `auth.request` &rArr; `GET` `songs/env/auth`. Parameters: `email`.
- `byid.request` &rArr; `POST` `songs/search/byid`
- `keywords.request` &rArr; `POST` `songs/search/keywords`
- `similar.request` &rArr; `POST` `songs/search/similar`
- `tags.request.get` &rArr; `GET` `songs/env/tags`. Parameters: `lang`
- `suggest.title` &rArr; `POST` `songs/search/suggest_title`
- `presyllables.request` &rArr; `POST` `songs/search/presyllables`
- `feedback.request` &rArr; `POST` `songs/env/feedback`.

Schemas and samples can be validated at: http://www.jsonschemavalidator.net
