# JSON schemas
## Name convention
Each JSON file has name with following structure:
    `{scope}`.`{symbolic id}`.`{direction}`.`{kind}`.`[variant]`.json
- `{scope}`
 
  > __frontend__ -- means that it is browser-server message  
  > __backend__ means that it is inner serverside message  
- `{symbolic id}` -- message unique id or name -- can contain dots inside as word delimiter
- `{direction}` -- __request__ or __response__
- `{kind}`

  > __schema__ -- it is JSON schema (see: http://json-schema.org)  
  > __sample__ -- it is sample of message in accordance with its schema  
- `{variant}` -- variant name or number of sample (optional) -- has the meaning for samples only.

## Frontend messages
`{symbolic id}` and `{direction}` parts only. 
- `byid.request` -- get songs by exact song id. Ids array is allowed.
- `keywords.request` -- search by keywords.
- `similar.request` -- search songs by rhythmic pattern, keywords and tags. Message `songs.response` will be returned.
- `songs.response` -- songs list. Each song with all attributes.  
It is response for `byid.request`, `keywords.request`, `similar.request`.
- `tags.request.get.url` -- this file contains URL for GET request for all tags list.
- `suggest.title.request` -- suggest titles by first letters.
- `suggest.title.response` -- suggested titles.
- `presyllables.request` -- song's text for syllables processing. Message `syllables.response` will be returned.
- `syllables.response` -- song with syllables array for each text row.

Schemas and samples can be validated at: http://www.jsonschemavalidator.net
