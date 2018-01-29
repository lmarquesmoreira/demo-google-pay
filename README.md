# demo-google-pay

#### Generating Key pam

openssl ecparam -name prime256v1 -genkey -noout -out key.pem

#### Reading values

openssl ec -in key.pem -pubout -text -noout


#### Generating public Key

openssl ec -in key.pem -pubout -text -noout 2> /dev/null | grep "pub:" -A5 | sed 1d | xxd -r -p | base64 | paste -sd "\0" -


#### Generating private Key

openssl pkcs8 -topk8 -inform PEM -outform DER -in key.pem -nocrypt | base64 | paste -sd "\0" -
