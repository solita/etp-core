(ns solita.etp.test-utils
  (:require [solita.etp.db]
            [schema-generators.generators :as g]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.geo :as geo-schema]
            [clojure.string :as str]
            [solita.etp.service.energiatodistus-pdf :as energiatodistus-pdf-service]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(defn unique-henkilotunnus-range [to]
  (->> (range 0 to)
       (map (partial format "%09d"))
       (map #(str % (common-schema/henkilotunnus-checksum %)))
       (map #(str (subs % 0 6) "-" (subs % 6 10)))))

(def laatija-generators
  {common-schema/Henkilotunnus       (g/always "130200A892S")
   laatija-schema/MuutToimintaalueet (g/always [0, 1, 2, 3, 17])
   common-schema/Date                (g/always (java.time.LocalDate/now))
   geo-schema/Maa                    (g/always "FI")
   common-schema/Url                 (g/always "https://example.com")})

(defn generate-kayttaja [n schema]
  (map #(assoc %1
               :email (str %2 "@example.com")
               :henkilotunnus (str/upper-case %3))
       (repeatedly n #(g/generate schema laatija-generators))
       (repeatedly n #(.toString (java.util.UUID/randomUUID)))
       (unique-henkilotunnus-range n)))

(defn sign-energiatodistus-pdf [db aws-s3-client whoami id]
  (let [language (energiatodistus-service/find-energiatodistus db id)]
    (doseq [language-code (energiatodistus-service/language-id->codes language)]
      (energiatodistus-pdf-service/find-energiatodistus-digest db aws-s3-client id language-code)
      (energiatodistus-pdf-service/sign-energiatodistus-pdf db aws-s3-client
                                                            (assoc whoami :sukunimi "Specimen-Potex")
                                                            id
                                                            language-code
                                                            {:signature "IAMcrYCDqC0nprcI2aKTZGAqHurktQYjw6IBh4gDrvl5FKrKczRlE07x8iwWd66O11J/LXuWj3xdNz3UTcPzvUBurT0VH4KDy9oGxeMbMLrJoWmD3gvzUrrRox/oA8/wKuTnqo/PIkJzkZFxty3zeh5ahNQAZEqXnUP+oBi524WlPNcSXA4EnTNlTm7FfJlWIUw8Ljo1ZqaFgOw7omTEeYJgBLiYAZgTSxeNkDMTogAqQA9jXnukUMDu7s/0APsZpFEU/kbYZM3Sz5XfgGHbq4p/zUzskTKqeMDfiJg8hGkMXfViLwS4cyriW/VyCm87WCBqkOeHD7whOv4KyVA/cw==",
                                                             :chain     ["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",
                                                                         "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",
                                                                         "MIIEHjCCAwagAwIBAgIDAdTAMA0GCSqGSIb3DQEBBQUAMIGlMQswCQYDVQQGEwJGSTEQMA4GA1UECBMHRmlubGFuZDEjMCEGA1UEChMaVmFlc3RvcmVraXN0ZXJpa2Vza3VzIFRFU1QxKTAnBgNVBAsTIENlcnRpZmljYXRpb24gQXV0aG9yaXR5IFNlcnZpY2VzMRkwFwYDVQQLExBWYXJtZW5uZXBhbHZlbHV0MRkwFwYDVQQDExBWUksgVEVTVCBSb290IENBMB4XDTAyMTIxNzE5MDA0NFoXDTIzMTIxNzE4NTg1MFowgaUxCzAJBgNVBAYTAkZJMRAwDgYDVQQIEwdGaW5sYW5kMSMwIQYDVQQKExpWYWVzdG9yZWtpc3RlcmlrZXNrdXMgVEVTVDEpMCcGA1UECxMgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkgU2VydmljZXMxGTAXBgNVBAsTEFZhcm1lbm5lcGFsdmVsdXQxGTAXBgNVBAMTEFZSSyBURVNUIFJvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDUxaN5pNBQqhtAm/XeVX+qbF2ILwiAumkt68A8JcrKOxOuUeXc3f6SWK/n+y0wZO/xr/c4ibElJTfvr+2Q33C5Z4gHS2NwJtK+/fbiwrZ667HUkCK11qehYC4dFeR7K01xKMGosDujp/ns6esldgBi0/30GNiFVKs7Kf19QjA+vGwMVtDdNVBdiJLtYxxV60gymHvD82Jwg1UFdvl1B2EkR5yrNdzBNbeeRGF6cBEbLJUvrHkNB5z8rng2qqz6rmuSBWl7SqChQRv/qP88Uz+nZaal3TCjg+IaGboURh1odW+u/JwQGVLzCutjVqyiqMMPwu65NUCDvoEnxeIVqPJLAgMBAAGjVTBTMA8GA1UdEwEB/wQFMAMBAf8wEQYJYIZIAYb4QgEBBAQDAgAHMA4GA1UdDwEB/wQEAwIBxjAdBgNVHQ4EFgQU3t5QCxdnwzepyZiINEjJMXnrbaEwDQYJKoZIhvcNAQEFBQADggEBAFSoft0eoN/19XnLGhWKLgveR/eTkaJX7ap63ffftnleky9exYSAFun5s8Rw7/Bf8WLftMa/YvGpfV64azcqzas2yo3HKKvTPHOegJbm5tMS3qVi8PGf6jxYcPeAFXMDg9SAex6GLxI5uoXflZ3TiDj64CvCSxHPCTwe2ybprVrRti6LwCO1iEOTGjvxuUSxVhRKcywZUgn1oVmcim63AvvfDcD8Ytx8xTlPPnibTkQzlwaWMCF+kDitksFbkOUdWYVoWFkb4vis/BYc6ZyjF1Wb1yEYNvVfL4/17kiZnZGxLNbM4Ygm2vUEZ8ualEtXfFCfv9DA8MeVIUfa2pTH6Tk="],}))))