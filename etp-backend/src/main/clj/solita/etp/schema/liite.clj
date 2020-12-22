(ns solita.etp.schema.liite
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(defn valid-attachment-url? [s]
  (re-matches #"^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*$)"
              s))

(def Url (schema/constrained schema/Str valid-attachment-url?))

(def Liite {:id              common-schema/Key
            :createtime      common-schema/Instant
            :author-fullname schema/Str
            :nimi            schema/Str
            :contenttype     (schema/maybe schema/Str)
            :url             (schema/maybe Url)})

(def LiiteLinkAdd {:nimi schema/Str
                   :url Url})

