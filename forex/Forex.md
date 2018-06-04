# Local proxy for Forex Rates

## Constraints and requirements
1. An internal user of the application should be able to ask for an exchange rate between 2 given currencies.
2. Rates must not be older than 5 minutes.
3. The application should at least support 10.000 requests per day.
4. The application should consume the [1forge API](https://1forge.com/forex-data-api/api-documentation), and do so using the [free tier](https://1forge.com/forex-data-api/pricing).

## Analysis
1forge free tier is limited to 1000 requests per day, and even though the proxy app (as it is today) only supports 72 currency pairs, it's possible to access more than 700 currency pairs through 1forge.
If the forex proxy worked as a simple proxy (i.e. forwarding all the requests to 1forge), the daily requests quota would be exhausted in a matter of hours or even minutes if peaks in demand are experienced. This means that adding a cache layer is absolutely necessary.

#### Refreshing the cache on-demand
One solution is caching/refreshing currency pairs on-demand. Only cache misses would trigger requests to 1forge for updated rates. This lazy strategy could be efficient under some circumstances. However, in a pessimistic scenario where the minimum number of daily requests (+10,000) are uniformly distributed amongst all currency pairs (currently 72), and uniformly distributed during the day, the time client requests for any given currency pair would be >10 min. Since a the rates must not be older than 5 minutes, the cache would always miss.

#### Periodical & asynchronous bulk cache refresh
A better solution consists on periodically launching an asynchronous job that requests rates for all the currency pair combinations. Since 1forge offers bulk requests on its API, the proxy service would be able to make requests and refresh the rates with delays of less than 2min.
If for any reason the number of allowed currency pairs per 1forge request is limited, the currency pairs could be splitted in 2 or more batches, the refresh time could be incremented (as long as it's < 5min), and/or several free tier api-keys/accounts could be used.

## Solution
I decided to implement the *Periodical & asynchronous bulk cache refresh* strategy. The key components in this solution are:
* [OneForgeLiveService](https://github.com/elyasib/interview/blob/attempt/forex/src/main/scala/forex/services/oneforge/OneForgeService.scala#L29) - In charge of scheduling the cache refreshes. It contains a Cache and a Client.
* [Cache](https://github.com/elyasib/interview/blob/attempt/forex/src/main/scala/forex/services/oneforge/Cache.scala) Provides an interface to interact with a thread-safe in-memory key-value store.
* [Client](https://github.com/elyasib/interview/blob/attempt/forex/src/main/scala/forex/services/oneforge/Client.scala) Exposes an interface to fetch rates from 1forge.
* [Intepreters](https://github.com/elyasib/interview/blob/attempt/forex/src/main/scala/forex/services/oneforge/Interpreters.scala) Know how to get the rates.

## TODOs
 - *Add retries with exponential back-off to the refresh job.* Wrapping the refresh task with a `TaskCircuitBreaker` is a relatively simple way of accomplishing this.
 - *Move back the cache update logic to the `Algebra` & `Intepreters`.* Right now the cache refresh logic lives in `OneForgeLiveService`, and sizeable portion of the `get` logic lives in the `Cache`. The latter makes [functional testing](https://github.com/elyasib/interview/blob/attempt/forex/src/test/scala/forex/main/ApplicationSpec.scala) very simple, but testing the cache refresh could be simpler. Leaving the core implementation details in the `Interpreter` would probably make the system more modular, and would allow more fine grained testing.

 ## How to
 ### Run tests
 `sbt test`
 ### Run the live service
 `sbt run` or from the sbt shell `reStart`

