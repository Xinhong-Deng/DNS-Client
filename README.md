# DNS Client

Java8

Invoke from terminal with the following input format

```
java DnsClient [-t timeout] [-r max-retries] [-p port] [-mx|-ns] @server name
```

The client is able to send queries for A, MX, and NS records and can interpret response with A records and CNAME records.
