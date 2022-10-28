(ns quadtree-cljc.core
  (:require [clojure.walk :as walk]))

(comment
  (def bounds {:x "double"
               :y "double"
               :width "double"
               :height "doubel"})

  (def quadtree {:bound bounds
                 :max-objects "int"
                 :max-levels "int"
                 :level "int"
                 :objects "vector of bounds"
                 :nodes "vector of quadtree maps"
                 :total "int"}))

(defn ->quadtree
  "Returns a map representing a quadtree"
  [bounds max-objects max-levels level objects nodes total]
  {:bounds bounds
   :max-objects max-objects
   :max-levels max-levels
   :level level
   :objects objects
   :nodes nodes
   :total total})

(defn ->bounds
  "Returns a map representing a bounding box"
  [x y width height]
  {:x x :y y :width width :height height})

(defn point?
  "Returns true if this object is a point in space
  as in it has no width or height"
  [{:keys [x y width height]}]
  (and (= width 0)
       (= height 0)))

(defn intersects?
  "Takes two object bounds and returns true if they
  intersect. "
  [obj-a obj-b]
  (let [{x-a :x y-a :y width-a :width height-a :height} obj-a
        {x-b :x y-b :y width-b :width height-b :height} obj-b
        max-x-for-a (+ x-a width-a)
        max-y-for-a (+ y-a height-a)
        max-x-for-b (+ x-b width-b)
        max-y-for-b (+ y-b height-b)]
    (not (or (< max-x-for-a x-b)
             (> x-a max-x-for-b)
             (< max-y-for-a y-b)
             (> y-a max-y-for-b)))))

(defn total-nodes
  "Returns the total number of sub-trees in `quadtree`. Valid
  sub-trees have a vector as their value to the parent's :nodes
  keyword."
  [quadtree]
  (let [nodes (:nodes quadtree)]
    (if (vector? nodes)
      (reduce + 1 (map total-nodes nodes))
      0)))

(defn split
  "Splits `quadtree` into 4 sub-nodes returns
  `quadtree` if this quadtree already has four nodes"
  [quadtree]
  (let [{:keys [nodes level bounds]} quadtree
        {:keys [x y width height]} bounds
        next-level (inc level)
        sub-width (/ width 2)
        sub-height (/ height 2)
        sub-quadtree (fn sub-quadtree
                       [quadtree x y width height level]
                       (merge quadtree {:bounds {:x x
                                                 :y y
                                                 :width width
                                                 :height height}
                                        :level level
                                        :objects []
                                        :nodes []}))]
    (if (= (count nodes) 4)
      quadtree
      (assoc quadtree
             :nodes
             [(sub-quadtree quadtree (+ x sub-width) y sub-width sub-height next-level)
              (sub-quadtree quadtree x y sub-width sub-height next-level)
              (sub-quadtree quadtree x (+ y sub-height) sub-width sub-height next-level)
              (sub-quadtree quadtree (+ x sub-width) (+ y sub-height) sub-width sub-height next-level)]))))

(defn get-quadrant
  "Determine the quadrant `bounds-obj` belongs to in `quadtree"
  [quadtree bounds-obj]
  (let [default -1
        qt-bounds (:bounds quadtree)
        {qt-x :x qt-y :y qt-width :width qt-height :height} qt-bounds
        {obj-x :x obj-y :y obj-width :width obj-height :height} bounds-obj
        vertical-midpoint (+ qt-x (/ qt-width 2))
        horizontal-midpoint (+ qt-y (/ qt-height 2))
        top-quadrant (and (< obj-y horizontal-midpoint)
                          (< (+ obj-y obj-height) horizontal-midpoint))
        bottom-quadrant (> obj-y horizontal-midpoint)
        left-quadrants (and (< obj-x vertical-midpoint)
                            (< (+ obj-x obj-width) vertical-midpoint))
        right-quadrants (> obj-x vertical-midpoint)]
    (cond
      (and left-quadrants top-quadrant) 1
      (and left-quadrants bottom-quadrant) 2
      (and right-quadrants top-quadrant) 0
      (and right-quadrants bottom-quadrant) 3)))

#_(defn insert
    "insert `bounds-obj` into the node, returning a freshly grown  quadtree.
  If the node exceeds the capacity, it will split and add all objects to
  their corresponding subnodes."
    [quadtree bounds-obj]
    (walk/postwalk
     (fn [subtree]
       (if (or (not (map? subtree))
               (pos? (count (:nodes subtree)))
               (:x subtree))
         subtree
         (let [{:keys [nodes objects bounds level max-levels
                       max-objects objects]} subtree
               {:keys [x y width height]} bounds
               split-qt (split subtree)
               quadrant (get-quadrant split-qt bounds-obj)
               object-quadrants (reduce (fn [acc obj]
                                          (let [quad (get-quadrant split-qt obj)]
                                            (update acc quad #(conj % obj))))
                                        {0 [] 1 [] 2 [] 3 []}
                                        (conj objects bounds-obj))
               new-nodes (vec
                          (map-indexed (fn [idx node]

                                         (merge node
                                                {:objects (get object-quadrants idx)})
                                         )
                                       (:nodes split-qt)))]
           (if (and (< level max-levels) (< (count objects) max-objects))
             (if (and (<= x (:x bounds-obj))
                      (<= y (:y bounds-obj))
                      (> width (:width bounds-obj))
                      (> height (:height bounds-obj)))
               (merge subtree {:objects (conj (:objects subtree) bounds-obj)})
               subtree)
             (merge split-qt {:nodes new-nodes
                              ;;:objects []
                              })))))
     quadtree))

(defn insert
  [quadtree bounds-obj]
  (let [{:keys [nodes objects bounds level max-levels
                max-objects objects]} quadtree
        all-objects (conj objects bounds-obj)]
    (if (pos? (count nodes))
      (insert (nth nodes (get-quadrant quadtree bounds-obj)) bounds-obj)
      (if (and (> (count all-objects) max-objects) (< level max-levels))
        (let [quadtree (if (pos? (count nodes)) quadtree (split quadtree))
              nodes (:nodes quadtree)
              object-quadrants (reduce (fn [acc obj]
                                         (let [quad (get-quadrant quadtree obj)]
                                           (update acc quad #(conj % obj))))
                                       {0 [] 1 [] 2 [] 3 []}
                                       all-objects)]
          (merge quadtree
                 {:objects (filter #(not (get-quadrant quadtree %)) all-objects)
                  :nodes (vec
                          (map-indexed (fn [idx node]

                                         (merge node
                                                {:objects (get object-quadrants idx)})
                                         )
                                       nodes))}))
        (merge quadtree {:objects all-objects})))))
