# cla-bot

Source code for the Metabase GitHub CLA bot. Create a GitHub app and Google Sheet with a CLA form and then go crazy.

## Env vars

```bash
GITHUB_CORE_CONTRIBUTORS_WHITELIST # i.e. camsaul,tlrobinson
GITHUB_INSTALLATION_ID # ID of GitHub app installation in given repo
GITHUB_PRIVATE_KEY # generate private key for app and copy source of PEM file here
GITHUB_REPO # i.e. metabase/metabase
GOOGLE_CLIENT_ID # Create an Google API client and then go thru OAuth steps in browser/REPL
GOOGLE_CLIENT_SECRET
GOOGLE_REFRESH_TOKEN
GOOGLE_SHEET_RANGE # i.e. 'Form Responses 1'!H2:H5000
GOOGLE_SPREADSHEET_ID # look in URL of Google Sheet
```

## Create the bot

Go in AWS and do it via UI or

```bash
lein uberjar &&
aws lambda create-function \
    --region us-east-1 \
    --function-name metabase-cla-bot  \
    --handler cla_bot.core::handleRequest \
    --runtime java8  \
    --memory 512 \
    --timeout 10 \
    --role arn:aws:iam::awsaccountid:role/lambda_basic_execution \
    --zip-file fileb://./target/cla-bot-1.0.0-standalone.jar
```

(Don't forget to add env vars as well!)

See also https://aws.amazon.com/blogs/compute/clojure/ and https://github.com/Charlynux/clojure-aws-lambda

## Update the bot

```bash
lein uberjar &&
aws lambda update-function-code \
  --region us-east-1 \
  --function-name metabase-cla-bot  \
  --zip-file fileb://./target/cla-bot-1.0.0-standalone.jar
```

## GitHub App debugging

Check out https://github.com/organizations/metabase/settings/apps/<app-name>/advanced

### License

Copyright © 2010 Metabase, Inc.

Distributed under the [Eclipse Public License](https://raw.githubusercontent.com/metabase/toucan/master/LICENSE.txt), same as Clojure.
