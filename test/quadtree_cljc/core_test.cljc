(ns quadtree-cljc.core-test
  (:require [quadtree-cljc.core :refer [->bounds
                                        ->quadtree
                                        total-nodes
                                        split
                                        get-quadrant
                                        insert
                                        insert-all
                                        retrieve
                                        retrieve-intersections
                                        retrieve-points]]
            #?(:clj [clojure.test :refer [deftest is testing run-tests]]
               :cljs [cljs.test :refer-macros [deftest is testing run-tests]])))

(deftest total-nodes-test
  (testing "Correct use case for total-nodes"
    (let [basic-quadtree (-> (->bounds 0 0 800 600)
                             (->quadtree 10 10 0 [] []))
          small-quadtree (-> (->bounds 0 0 800 600)
                             (->quadtree 10 10 0 [] [])
                             (insert-all [{:x 0 :y 0 :width 10 :height 10}
                                          {:x 0 :y 5 :width 10 :height 10}]))
          medium-quadtree (-> (->bounds 0 0 800 600)
                              (->quadtree 5 5 0 [] [])
                              (insert-all [{:x 0 :y 0 :width 10 :height 10}
                                           {:x 0 :y 5 :width 10 :height 10}
                                           {:x 100 :y 150 :width 10 :height 10}
                                           {:x 110 :y 160 :width 10 :height 10}
                                           {:x 160 :y 390 :width 10 :height 10}]))
          medium-qt-max-objects (insert medium-quadtree {:x 150 :y 380
                                                         :width 10 :height 10})
          deep-quadtree (-> (->bounds 0 0 800 600)
                            (->quadtree 1 100 0 [] [])
                            (insert-all [{:x 0 :y 0 :width 10 :height 10}
                                         {:x 0 :y 5 :width 10 :height 10}]))]
      (is (= 1 (total-nodes basic-quadtree)))
      (is (= 1 (total-nodes small-quadtree)))
      (is (= 1 (total-nodes medium-quadtree)))
      (is (= 5 (total-nodes medium-qt-max-objects)))
      (is (= 25 (total-nodes deep-quadtree)))
      (is (= 1 (total-nodes {:nodes []})))))

  (testing "Incorrect use cases for total-nodes"
    (is (= 0 (total-nodes nil)))
    (is (= 0 (total-nodes {:nodes nil})))
    (is (= 0 (total-nodes [])))
    (is (= 0 (total-nodes 111)))
    (is (= 0 (total-nodes 1.000000)))
    (is (= 0 (total-nodes #{:nodes})))))

(deftest split-test
  (testing "Correct use case for split"
    (let [quadtree (-> (->bounds 0 0 800 600)
                       (->quadtree 10 10 0 [] []))]
      (is (= {:bounds {:x 0, :y 0, :width 800, :height 600},
              :max-objects 10,
              :max-levels 10,
              :level 0,
              :objects [],
              :nodes
              [{:bounds {:x 400, :y 0, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 0, :y 0, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 0, :y 300, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 400, :y 300, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}]} (split quadtree)))))

  (testing "Incorrect use case for split"
    (let [one-node {:bounds {:x 0, :y 0, :width 800, :height 600},
                    :max-objects 10,
                    :max-levels 10,
                    :level 0,
                    :objects [],
                    :nodes
                    [{:bounds {:x 400, :y 0, :width 400, :height 300},
                      :max-objects 10,
                      :max-levels 10,
                      :level 1,
                      :objects [],
                      :nodes []}]}
          no-nodes {:bounds {:x 0, :y 0, :width 800, :height 600},
                    :max-objects 10,
                    :max-levels 10,
                    :level 0,
                    :objects []}]
      (is (= {:bounds {:x 0, :y 0, :width 800, :height 600},
              :max-objects 10,
              :max-levels 10,
              :level 0,
              :objects [],
              :nodes
              [{:bounds {:x 400, :y 0, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 0, :y 0, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 0, :y 300, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 400, :y 300, :width 400, :height 300},
                :max-objects 10,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}]} (split one-node)))
      (is (thrown? NullPointerException (split [])))
      (is (thrown? NullPointerException (split {})))
      (is (thrown? NullPointerException (split 231321654))))))

(deftest insert-tests
  (testing "insert via insert-all"
    (let [basic-quadtree (-> (->bounds 0 0 800 600)
                             (->quadtree 10 10 0 [] []))
          medium-quadtree (-> (->bounds 0 0 800 600)
                              (->quadtree 5 5 0 [] []))
          max-levels-quadtree (-> (->bounds 0 0 800 600)
                                  (->quadtree 1 10 0 [] []))]
      (is (= {:bounds {:x 0, :y 0, :width 800, :height 600},
              :max-objects 5,
              :max-levels 5,
              :level 0,
              :objects [],
              :nodes
              [{:bounds {:x 400, :y 0, :width 400, :height 300},
                :max-objects 5,
                :max-levels 5,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 0, :y 0, :width 400, :height 300},
                :max-objects 5,
                :max-levels 5,
                :level 1,
                :objects
                [{:x 0, :y 0, :width 10, :height 10}
                 {:x 0, :y 5, :width 10, :height 10}
                 {:x 100, :y 150, :width 10, :height 10}
                 {:x 110, :y 160, :width 10, :height 10}],
                :nodes []}
               {:bounds {:x 0, :y 300, :width 400, :height 300},
                :max-objects 5,
                :max-levels 5,
                :level 1,
                :objects
                [{:x 160, :y 390, :width 10, :height 10}
                 {:x 160, :y 400, :width 10, :height 10}],
                :nodes []}
               {:bounds {:x 400, :y 300, :width 400, :height 300},
                :max-objects 5,
                :max-levels 5,
                :level 1,
                :objects [],
                :nodes []}]} (insert-all medium-quadtree [{:x 0 :y 0 :width 10 :height 10}
                                                          {:x 0 :y 5 :width 10 :height 10}
                                                          {:x 100 :y 150 :width 10 :height 10}
                                                          {:x 110 :y 160 :width 10 :height 10}
                                                          {:x 160 :y 390 :width 10 :height 10}
                                                          {:x 160 :y 400 :width 10 :height 10}]))
          (= {:bounds {:x 0, :y 0, :width 800, :height 600},
              :max-objects 1,
              :max-levels 10,
              :level 0,
              :objects [],
              :nodes
              [{:bounds {:x 400, :y 0, :width 400, :height 300},
                :max-objects 1,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 0, :y 0, :width 400, :height 300},
                :max-objects 1,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes
                [{:bounds {:x 200, :y 0, :width 200, :height 150},
                  :max-objects 1,
                  :max-levels 10,
                  :level 2,
                  :objects [],
                  :nodes []}
                 {:bounds {:x 0, :y 0, :width 200, :height 150},
                  :max-objects 1,
                  :max-levels 10,
                  :level 2,
                  :objects [],
                  :nodes
                  [{:bounds {:x 100, :y 0, :width 100, :height 75},
                    :max-objects 1,
                    :max-levels 10,
                    :level 3,
                    :objects [],
                    :nodes []}
                   {:bounds {:x 0, :y 0, :width 100, :height 75},
                    :max-objects 1,
                    :max-levels 10,
                    :level 3,
                    :objects [],
                    :nodes
                    [{:bounds {:x 50, :y 0, :width 50, :height 75/2},
                      :max-objects 1,
                      :max-levels 10,
                      :level 4,
                      :objects [],
                      :nodes []}
                     {:bounds {:x 0, :y 0, :width 50, :height 75/2},
                      :max-objects 1,
                      :max-levels 10,
                      :level 4,
                      :objects [],
                      :nodes
                      [{:bounds {:x 25, :y 0, :width 25, :height 75/4},
                        :max-objects 1,
                        :max-levels 10,
                        :level 5,
                        :objects [],
                        :nodes []}
                       {:bounds {:x 0, :y 0, :width 25, :height 75/4},
                        :max-objects 1,
                        :max-levels 10,
                        :level 5,
                        :objects
                        [{:x 0, :y 0, :width 10, :height 10}
                         {:x 0, :y 5, :width 10, :height 10}],
                        :nodes
                        [{:bounds {:x 25/2, :y 0, :width 25/2, :height 75/8},
                          :max-objects 1,
                          :max-levels 10,
                          :level 6,
                          :objects [],
                          :nodes []}
                         {:bounds {:x 0, :y 0, :width 25/2, :height 75/8},
                          :max-objects 1,
                          :max-levels 10,
                          :level 6,
                          :objects [],
                          :nodes []}
                         {:bounds {:x 0, :y 75/8, :width 25/2, :height 75/8},
                          :max-objects 1,
                          :max-levels 10,
                          :level 6,
                          :objects [],
                          :nodes []}
                         {:bounds {:x 25/2, :y 75/8, :width 25/2, :height 75/8},
                          :max-objects 1,
                          :max-levels 10,
                          :level 6,
                          :objects [],
                          :nodes []}]}
                       {:bounds {:x 0, :y 75/4, :width 25, :height 75/4},
                        :max-objects 1,
                        :max-levels 10,
                        :level 5,
                        :objects [],
                        :nodes []}
                       {:bounds {:x 25, :y 75/4, :width 25, :height 75/4},
                        :max-objects 1,
                        :max-levels 10,
                        :level 5,
                        :objects [],
                        :nodes []}]}
                     {:bounds {:x 0, :y 75/2, :width 50, :height 75/2},
                      :max-objects 1,
                      :max-levels 10,
                      :level 4,
                      :objects [],
                      :nodes []}
                     {:bounds {:x 50, :y 75/2, :width 50, :height 75/2},
                      :max-objects 1,
                      :max-levels 10,
                      :level 4,
                      :objects [],
                      :nodes []}]}
                   {:bounds {:x 0, :y 75, :width 100, :height 75},
                    :max-objects 1,
                    :max-levels 10,
                    :level 3,
                    :objects [],
                    :nodes []}
                   {:bounds {:x 100, :y 75, :width 100, :height 75},
                    :max-objects 1,
                    :max-levels 10,
                    :level 3,
                    :objects [],
                    :nodes []}]}
                 {:bounds {:x 0, :y 150, :width 200, :height 150},
                  :max-objects 1,
                  :max-levels 10,
                  :level 2,
                  :objects [],
                  :nodes []}
                 {:bounds {:x 200, :y 150, :width 200, :height 150},
                  :max-objects 1,
                  :max-levels 10,
                  :level 2,
                  :objects [],
                  :nodes []}]}
               {:bounds {:x 0, :y 300, :width 400, :height 300},
                :max-objects 1,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}
               {:bounds {:x 400, :y 300, :width 400, :height 300},
                :max-objects 1,
                :max-levels 10,
                :level 1,
                :objects [],
                :nodes []}]} (insert-all max-levels-quadtree [{:x 0 :y 0 :width 10 :height 10}
                                                              {:x 0 :y 5 :width 10 :height 10}])))
      (is (= {:bounds {:x 0, :y 0, :width 800, :height 600},
              :max-objects 10,
              :max-levels 10,
              :level 0,
              :objects [{:x 0, :y 0, :width 10, :height 10}],
              :nodes []} (insert basic-quadtree {:x 0 :y 0 :width 10 :height 10})))
      (is (= {:bounds {:x 0, :y 0, :width 800, :height 600},
              :max-objects 10,
              :max-levels 10,
              :level 0,
              :objects [],
              :nodes []} (insert-all basic-quadtree []))))))
