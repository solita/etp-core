(ns solita.etp.schema.liite
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Liite {:id              common-schema/Key
            :createtime      common-schema/Instant
            :author-fullname schema/Str
            :nimi            schema/Str
            :contenttype     (schema/maybe schema/Str)
            :url             (schema/maybe common-schema/Url)})

(def LiiteLinkAdd {:nimi schema/Str
                   :url common-schema/Url})

