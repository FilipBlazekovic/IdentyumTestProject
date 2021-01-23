# IdentyumTestProject
Web Service written in Spring Boot using an H2 in-memory database

**RUNNING THE APPLICATION**

```
maven clean
maven install
java -Dvonage_api_key="REPLACE_ME" -Dvonage_api_secret="REPLACE_ME" -jar IdentyumTestProject-0.0.1-SNAPSHOT.jar
```

&nbsp;
&nbsp;

**CURL REQUEST EXAMPLES**

**REGISTRATION** [returns session cookie (performs autologin)]

```
curl -i -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"Password1234"}' http://127.0.0.1:10000/identyum/register

Example response:
----------------
HTTP/1.1 200
Set-Cookie: sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1
Content-Length: 0
Date: Sat, 23 Jan 2021 12:08:53 GMT
```

**LOGIN** [returns session cookie]

```
curl -i -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"Password1234"}' http://127.0.0.1:10000/identyum/login

Example response:
----------------
HTTP/1.1 200 
Set-Cookie: sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1
Content-Length: 0
Date: Sat, 23 Jan 2021 12:12:20 GMT
```

**PHONE-VERIFICATION -> STEP 1: ENTERING PHONE NUMBER**

```
curl -i -X POST --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" -H "Content-Type: application/json" -d '{"phoneNumber":"385991111222"}' http://127.0.0.1:10000/identyum/verify
```

**PHONE-VERIFICATON -> STEP 2: ENTERING OTP**

```
curl -i -X POST --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" -H "Content-Type: application/json" -d '{"verificationCode":"1234"}' http://127.0.0.1:10000/identyum/submit-otp
```

**RETRIEVING USER DETAILS**

```
curl -i --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" http://127.0.0.1:10000/identyum/details
```

**RETRIEVING UPLOADED IMAGES**

Images are stored/returned as base64 encoded strings for easier testing from the terminal.

```
curl -i --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" http://127.0.0.1:10000/identyum/my-images
```

**RETRIEVING IMAGES SHARED WITH THIS USER**

Images are stored/returned as base64 encoded strings for easier testing from the terminal.

```
curl -i --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" http://127.0.0.1:10000/identyum/shared-images
```

**UPLOADING AN IMAGE**

```
curl -i --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" --request POST -H "Content-Type:multipart/form-data" --form file="@/path/image.jpeg" http://127.0.0.1:10000/identyum/image
```

**DELETING AN IMAGE**

```
curl -i -X "DELETE" --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" http://127.0.0.1:10000/identyum/image/IMAGE_ID
```

**SHARING AN IMAGE**

```
curl -i -X POST --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" -H "Content-Type: application/json" -d '{"imageId":1,"username":"john"}' http://127.0.0.1:10000/identyum/share
```

**UNSHARING AN IMAGE**

```
curl -i -X POST --cookie "sessionID=4693976F96890FCCD3A15268955367ACD9538048E6E1EBD12D434E321D0BA4D1" -H "Content-Type: application/json" -d '{"imageId":1,"username":"john"}' http://127.0.0.1:10000/identyum/unshare
```

