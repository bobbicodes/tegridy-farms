# tegridy-farms

HTTP Integration with GitHub and Twitter

Upon execution, pull requests to this repo will be posted to the [TegridyFarms2](https://twitter.com/TegridyFarms2) twitter account.

## Configuration

You must supply your Github and Twitter credentials in a file named `config.edn`:

```edn
{:github {:oauth-token "oauth token"}
 :twitter {:api-key "api key"
           :api-secret-key "api secret key"
           :access-token "access token"
           :access-token-secret "access token secret"}}
```

## Usage

* Make sure you have the Clojure CLI tools installed, and run:

```bash
clj -m tegridy-farms.core
```
