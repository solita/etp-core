(ns solita.common.logic
  "This namespace contains higher order logic functions.
  The purpose is to augment clojure core logic to ramda level.")

(defn if*
  "Higher order ramda style if function."
  ([predicate then-fn] (if* predicate then-fn (constantly nil)))
  ([predicate then-fn else-fn]
    #(if (predicate %) (then-fn %) (else-fn %))))

(defn when*
  "Higher order ramda style when function."
  ([predicate then-fn]
   #(if (predicate %) (then-fn %) %)))

(defn unless*
  "Higher order ramda style unless function."
  ([predicate then-fn]
   #(if (predicate %) (then-fn %) %)))