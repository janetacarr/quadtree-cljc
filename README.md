# quadtree-cljc

[![Clojars Project](https://img.shields.io/clojars/v/com.janetacarr/quadtree-cljc.svg)](https://clojars.org

A quadtree implementation for clojure(script) inspired by [this](https://github.com/janetacarr/quadtree-go/blob/master/quadtree.go) implementation of a quadtree, but with a dash of functional programming flair added by yours truly.

All transform functions take a quadtree and return a new one. There's nothing fancy going on, just good ol' Clojure maps. So you can break the tree if you're not careful. I recommend using the built in functions to create your quadtree and pass it around in a binding.

## Getting Started

To start building a quadtree, create a quadtree using `->quadtree`  with a bounds for the root node.

``` clojure
user> (use 'quadtree-cljc.core)
user> (def tree (-> (->bounds 0 0 800 600)
                    (->quadtree 5 5 0 [] [])))
#'user/tree
user> tree
{:bounds {:x 0, :y 0, :width 800, :height 600},
 :max-objects 5,
 :max-levels 5,
 :level 0,
 :objects [],
 :nodes []}
user>
```

Now you can insert a node into the tree like so:

``` clojure
user> (insert tree {:x 5 :y 10 :width 10 :height 10})
{:bounds {:x 0, :y 0, :width 800, :height 600},
 :max-objects 1,
 :max-levels 10,
 :level 0,
 :objects [{:x 5, :y 10, :width 10, :height 10}],
 :nodes []}
user>
```

Or you can insert multiple with `insert-all`:

``` clojure
user> (def tree (-> (->bounds 0 0 800 600)
                    (->quadtree 5 5 0 [] [])
                    (insert-all [{:x 0 :y 0 :width 10 :height 10}
                                 {:x 0 :y 5 :width 10 :height 10}
                                 {:x 100 :y 150 :width 100 :height 100}
                                 {:x 160 :y 390 :width 10 :height 10}])))
#'user/tree
user>
```

And finally we can even find colliding / intersection objects in the tree:

``` clojure
user> (-> (->bounds 0 0 800 600)
          (->quadtree 5 5 0 [] [])
          (insert-all [{:x 0 :y 0 :width 10 :height 10}
                       {:x 0 :y 5 :width 10 :height 10}
                       {:x 100 :y 150 :width 100 :height 100}
                       {:x 160 :y 390 :width 10 :height 10}])
          (retrieve-intersections {:x 110 :y 160 :width 10 :height 10}))
[{:x 100, :y 150, :width 100, :height 100}]
user>
```

If you were using this in a game, you might build up your quadtree on every game frame,
inserting all your entities bounds, and then decide if you should update your entities.
