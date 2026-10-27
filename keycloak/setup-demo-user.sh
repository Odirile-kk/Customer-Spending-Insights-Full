
#!/usr/bin/env bash
set -Eeuo pipefail

KC=/opt/keycloak/bin/kcadm.sh
SERVER=http://keycloak:8080
REALM=customer-insights
CLIENT_ID=customer-insights-rest-api

: "${KEYCLOAK_ADMIN:?Missing admin username}"
: "${KEYCLOAK_ADMIN_PASSWORD:?Missing admin password}"
: "${DEMO_USERNAME:?Missing demo username}"
: "${DEMO_EMAIL:?Missing demo email}"
: "${DEMO_USER_PASSWORD:?Missing demo password}"
: "${DEMO_FIRST_NAME:?Missing first name}"
: "${DEMO_LAST_NAME:?Missing last name}"
: "${DEMO_CUSTOMER_ID:=12345}"

# Return an ID for an exact field/value match using Python's JSON parser.
# Python is not guaranteed to be installed in the Keycloak image, so use
# Keycloak's CLI JSON output and a small, structure-aware Bash helper instead.
find_id_by_name() {
  local collection="$1"
  local field="$2"
  local value="$3"
  local data

  data=$("$KC" get "$collection" -r "$REALM")

  printf '%s\n' "$data" |
    tr '\n' ' ' |
    sed 's/},[[:space:]]*/}\n/g' |
    grep -F "\"$field\" : \"$value\"" |
    head -n 1 |
    grep -oE '"id"[[:space:]]*:[[:space:]]*"[^"]+"' |
    head -n 1 |
    sed 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/' || true
}

find_scope_id() {
  local name="$1"
  find_id_by_name client-scopes name "$name"
}

ensure_scope() {
  local name="$1"
  local scope_id

  scope_id="$(find_scope_id "$name")"

  if [ -n "$scope_id" ]; then
    echo "Scope exists: $name" >&2
    printf '%s' "$scope_id"
    return
  fi

  echo "Creating scope: $name" >&2

  "$KC" create client-scopes -r "$REALM" \
    -s "name=$name" \
    -s protocol=openid-connect \
    -s 'attributes."include.in.token.scope"=true' \
    -s 'attributes."display.on.consent.screen"=false'

  scope_id="$(find_scope_id "$name")"

  if [ -z "$scope_id" ]; then
    echo "ERROR: Could not find scope after creating it: $name" >&2
    return 1
  fi

  printf '%s' "$scope_id"
}

ensure_standard_mapper() {
  local scope_id="$1"
  local mapper_name="$2"
  local user_attribute="$3"
  local claim_name="$4"
  local mappers

  mappers="$("$KC" get "client-scopes/$scope_id/protocol-mappers/models" -r "$REALM")"

  if printf '%s' "$mappers" |
      grep -Fq "\"name\" : \"$mapper_name\""; then
    echo "Mapper exists: $mapper_name"
    return
  fi

  echo "Creating mapper: $mapper_name"

  "$KC" create "client-scopes/$scope_id/protocol-mappers/models" \
    -r "$REALM" \
    -s "name=$mapper_name" \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-usermodel-attribute-mapper \
    -s 'consentRequired=false' \
    -s "config.\"user.attribute\"=$user_attribute" \
    -s "config.\"claim.name\"=$claim_name" \
    -s 'config."jsonType.label"=String' \
    -s 'config."access.token.claim"=true' \
    -s 'config."id.token.claim"=true' \
    -s 'config."userinfo.token.claim"=true'
}

attach_default_scope() {
  local scope_id="$1"
  local scope_name="$2"
  local attached

  attached="$("$KC" get "clients/$CLIENT_UUID/default-client-scopes" -r "$REALM")"

  if printf '%s' "$attached" |
      grep -Fq "\"id\" : \"$scope_id\""; then
    echo "Already attached: $scope_name"
    return
  fi

  echo "Attaching default scope: $scope_name"

  "$KC" update \
    "clients/$CLIENT_UUID/default-client-scopes/$scope_id" \
    -r "$REALM"
}

echo "Waiting for Keycloak..."

READY=false
for attempt in $(seq 1 60); do
  if "$KC" config credentials \
      --server "$SERVER" \
      --realm master \
      --user "$KEYCLOAK_ADMIN" \
      --password "$KEYCLOAK_ADMIN_PASSWORD" \
      >/dev/null 2>&1; then
    READY=true
    break
  fi
  sleep 5
done

if [ "$READY" != true ]; then
  echo "ERROR: Keycloak admin API is not ready."
  exit 1
fi

echo "Checking client: $CLIENT_ID"

CLIENT_UUID="$(find_id_by_name clients clientId "$CLIENT_ID")"

if [ -z "$CLIENT_UUID" ]; then
  echo "ERROR: Client $CLIENT_ID does not exist in realm $REALM."
  exit 1
fi

echo "Checking customer ID scope..."
CUSTOMER_SCOPE_ID="$(ensure_scope customer-id-scope)"

ensure_standard_mapper \
  "$CUSTOMER_SCOPE_ID" \
  customer-id-mapper \
  customer_id \
  customer_id

echo "Checking profile and email scopes..."

PROFILE_SCOPE_ID="$(ensure_scope profile)"
EMAIL_SCOPE_ID="$(ensure_scope email)"

# Add standard OIDC profile claims.
ensure_standard_mapper "$PROFILE_SCOPE_ID" given-name givenName given_name
ensure_standard_mapper "$PROFILE_SCOPE_ID" family-name lastName family_name
ensure_standard_mapper "$PROFILE_SCOPE_ID" full-name username name

# Add the email claim.
ensure_standard_mapper "$EMAIL_SCOPE_ID" email email email

echo "Attaching default client scopes..."

attach_default_scope "$PROFILE_SCOPE_ID" profile
attach_default_scope "$EMAIL_SCOPE_ID" email
attach_default_scope "$CUSTOMER_SCOPE_ID" customer-id-scope

echo "Checking demo user: $DEMO_USERNAME"

USER_ID="$(
  "$KC" get users -r "$REALM" -q "username=$DEMO_USERNAME" |
    tr '\n' ' ' |
    sed 's/},[[:space:]]*/}\n/g' |
    grep -F "\"username\" : \"$DEMO_USERNAME\"" |
    head -n 1 |
    grep -oE '"id"[[:space:]]*:[[:space:]]*"[^"]+"' |
    head -n 1 |
    sed 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/' || true
)"

if [ -n "$USER_ID" ]; then
  echo "Demo user already exists; leaving password unchanged."

  "$KC" update "users/$USER_ID" -r "$REALM" \
    -s "email=$DEMO_EMAIL" \
    -s "firstName=$DEMO_FIRST_NAME" \
    -s "lastName=$DEMO_LAST_NAME" \
    -s enabled=true \
    -s "attributes.customer_id=[\"$DEMO_CUSTOMER_ID\"]"
else
  echo "Creating demo user..."

  USER_ID="$(
    "$KC" create users -r "$REALM" -i \
      -s "username=$DEMO_USERNAME" \
      -s "email=$DEMO_EMAIL" \
      -s "firstName=$DEMO_FIRST_NAME" \
      -s "lastName=$DEMO_LAST_NAME" \
      -s enabled=true \
      -s emailVerified=true \
      -s "attributes.customer_id=[\"$DEMO_CUSTOMER_ID\"]"
  )"

  if [ -z "$USER_ID" ]; then
    echo "ERROR: User creation did not return an ID."
    exit 1
  fi

  "$KC" set-password -r "$REALM" \
    --username "$DEMO_USERNAME" \
    --new-password "$DEMO_USER_PASSWORD"

  echo "Demo user created and password configured."
fi

echo "Keycloak initialization complete."
echo "Username: $DEMO_USERNAME"
echo "Customer ID: $DEMO_CUSTOMER_ID"
