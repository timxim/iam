INSERT INTO authentication_holder (id, user_auth_id, approved, redirect_uri, client_id) VALUES
(1, 1, TRUE, NULL, 'token-exchange-subject');

INSERT INTO saved_user_auth_authority (owner_id, authority) VALUES
(1, 'ROLE_USER');

INSERT INTO authentication_holder_scope (owner_id, scope) VALUES
(1, 'openid'),
(1, 'offline_access'),
(1, 'profile');

INSERT INTO authentication_holder_extension (owner_id, extension, VAL) VALUES
(1, 'AUTH_TIMESTAMP', TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP()));

INSERT INTO authentication_holder_request_parameter (owner_id, param, val) VALUES
(1, 'scope', 'openid profile offline_access'),
(1, 'grant_type', 'password'),
(1, 'username', 'vianello');

INSERT INTO saved_user_auth (id, name, authenticated, source_class) VALUES
(1, 'vianello', TRUE, 'org.springframework.security.authentication.UsernamePasswordAuthenticationToken');

INSERT INTO access_token (id, token_value, expiration, token_type, refresh_token_id, client_id, auth_holder_id, id_token_id, approved_site_id) VALUES
(1, 'eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI4MGU1ZmI4ZC1iN2M4LTQ1MWEtODliYS0zNDZhZTI3OGE2NmYiLCJpc3MiOiJodHRwOlwvXC9sb2NhbGhvc3Q6ODA4MFwvIiwiZXhwIjoxNTQyODEwNzYwLCJpYXQiOjE1NDI4MDcxNjAsImp0aSI6ImEyNTJjMmE5LWFhZTEtNDNmZi04ZjNlLTAwY2JkODU0MTUwYSJ9.hm5GhHd1FOeeUkndGFKL8r8rNcpcmS_XFDyB6a4LFUO9uLqhC08-d1qDkpesg6MKTeTBuygA4ihX6khc8PGdfZRAtfQiYQJNqGJ72nOKZ-MNKBNqn0ztVEXvu9QTeGMKSyhFSOc2sScclZtCwUCIQfFXJsa_XjzpRdE_DuYOP0w', TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP()), 'Bearer', 1, 9, 1, NULL, NULL);

INSERT INTO refresh_token (id, token_value, expiration, auth_holder_id, client_id) VALUES
(1, 'eyJhbGciOiJub25lIn0.eyJqdGkiOiIwNDE4YWNkMi1hNWY3LTQ3YWItYTljZS1mZDVlZWYzNjI0MjcifQ.', NULL, 1, 9);
