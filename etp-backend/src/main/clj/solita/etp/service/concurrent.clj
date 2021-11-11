(ns solita.etp.service.concurrent
  (:require [solita.etp.exception-email-handler :as exception-email-handler]
            [clojure.tools.logging :as log]))

(defn run-background
  "Executes the given function asynchronously as a background service.
  Returns immediately nil and exceptions are only logged.
  Execution is implemented using clojure future."
  [fn error-description]
  (future
    (try
      (fn)
      (catch Throwable t
        (log/error t error-description (or (ex-data t) ""))
        (exception-email-handler/exception-handler t))))
  nil)