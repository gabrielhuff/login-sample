### POST `/token`

Issue a token using the given credentials with basic auth. For instance, a cliend can request:

```http
POST /token

Authorization: Basic dXNlcjpwYXNzd29yZA==
```

And the server can reply with:

```http
200 OK

{
  "token": "424242"
}
```

Possible errors:

- **400 Bad Request**: If the authentication schema is invalid (i.e. not `Basic`)
- **401 Unauthorized**: If the credentials are invalid or not informed

### POST `/user`

Register a user. The user data is passed on the request body and the new credentials are sent as basic auth header. The following should be satisfied:

- All the skill fields should be floating points between 0 and 1 (both inclusive)
- The username from both credentials and user data must match.
- The username and password should both contain only alphanumeric characters and have between 1 and 16 characters

```http
POST /user

Authorization: Basic dXNlcjpwYXNzd29yZA==
Content-Type: application/json

{
  "username": "user",
  "skill_rx_java": 0.6,
  "skill_docker": 0.5,
  "skill_kotlin": 0.7
}
```

And the server can reply the following successful response (body will **ALWAYS** be the same as the input):

```http
201 CREATED

{
  "username": "username",
  "skill_rx_java": 0.6,
  "skill_docker": 0.5,
  "skill_kotlin": 0.7
}
```

Possible errors:

- **400 Bad Request**: If there's anything wrong with the request format (e.g. invalid JSON body, invalid credentials / user data inputs, invalid authentication schema)
- **401 Unauthorized**: If the username provided on the credentials is different than the one provided as user data or credentials are not informed
- **409 Conflict**: If the username is already being used
- **415 Unsupported Media Type**: If the body type is not `application/json`

### GET `/user`

Get the user data from an authenticated user. The client must authenticate itself with a valid token (i.e. issued on a `POST /token` request), passing it on the `Authorizarion` header using the format `Bearer <token>`. For instance, the given request:

```http
GET /user

Authorization: Bearer 424242
```

Results on the following response, in case of success:

```http
200 OK

{
  "username": "username",
  "skill_rx_java": 0.6,
  "skill_docker": 0.5,
  "skill_kotlin": 0.7
}
```

Possible errors:

- **400 Bad Request**: If the authentication schema is invalid (i.e. not `Bearer`)
- **401 Unauthorized**: If the token provided is invalid (can be expired) or not informed