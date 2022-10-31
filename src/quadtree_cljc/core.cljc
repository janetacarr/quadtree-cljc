(ns quadtree-cljc.core)

(defn ->quadtree
  "Returns a map representing a quadtree"
  [bounds max-objects max-levels level objects nodes]
  {:bounds bounds
   :max-objects max-objects
   :max-levels max-levels
   :level level
   :objects objects
   :nodes nodes})

(defn ->bounds
  "Returns a map representing a bounding box"
  [x y width height]
  {:x x :y y :width width :height height})

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

(defn insert
  "Insert `bounds-obj` into the node, returning a freshly grown quadtree.
  If the node exceeds the capacity, it will split and add all objects to
  their corresponding subnodes."
  [quadtree bounds-obj]
  (let [{:keys [nodes objects bounds
                level max-levels
                max-objects objects]} quadtree
        all-objects (conj objects bounds-obj)]
    (if (pos? (count nodes))
      (as-> quadtree quadtree
        (assoc quadtree :objects [])
        (reduce (fn [quadtree obj]
                  (let [quadrant (get-quadrant quadtree obj)
                        nodes (:nodes quadtree)]
                    (if quadrant
                      (merge quadtree {:nodes (assoc nodes
                                                     quadrant
                                                     (insert (nth nodes quadrant)
                                                             obj))})
                      (update quadtree :objects #(conj % obj)))))
                quadtree
                all-objects))
      (if (and (> (count all-objects) max-objects) (< level max-levels))
        (let [quadtree (if (empty? nodes) (split quadtree) quadtree)]
          (insert quadtree bounds-obj))
        (merge quadtree {:objects all-objects})))))

(defn insert-all
  "Takes a `quadtree` and inserts all bounds objects from the
  bounds-objs vector."
  [quadtree bounds-objs]
  (reduce (fn [quadtree obj]
            (merge quadtree (insert quadtree obj)))
          quadtree
          bounds-objs))

(defn retrieve
  "Retrieves a vector of all the bounds objects that could collide with
  `bounds-obj` in `quadtree`."
  [quadtree bounds-obj]
  (->> (let [quadrant (get-quadrant quadtree bounds-obj)
             nodes (:nodes quadtree)]
         (if (pos? (count nodes))
           (if quadrant
             (retrieve (nth nodes quadrant) bounds-obj)
             (mapv #(retrieve % bounds-obj) nodes))
           (:objects quadtree)))
       (flatten)
       (vec)))

(defn point?
  "Returns true if this object is a point in space
  as in it has no width or height"
  [{:keys [x y width height]}]
  (and (= width 0) (= height 0)))

(defn retrieve-points
  "Returns a vector of all the points that collide/intersect
  with `bounds-obj` in the `quadtree`."
  [quadtree bounds-obj]
  (filterv #(and (= (:x %) (:x bounds-obj))
                 (= (:y %) (:y bounds-obj))
                 (point? %)) (retrieve quadtree bounds-obj)))

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

(defn retrieve-intersections
  "Returns all a vector of all the objects that collide/intersect
  with `bounds-obj` in the `quadtree`."
  [quadtree bounds-obj]
  (filterv #(intersects? % bounds-obj) (retrieve quadtree bounds-obj)))
