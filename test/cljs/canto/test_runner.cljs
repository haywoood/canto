(ns canto.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [canto.core-test]
   [canto.common-test]))

(enable-console-print!)

(doo-tests 'canto.core-test
           'canto.common-test)
