(ns solita.etp.schema.user
  (:require [schema.core :as schema]))

(def User {:id schema/Num :username schema/Str})

