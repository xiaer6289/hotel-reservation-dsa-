# Hotel Reservation Prototype

Architecture: **ECB (Entity-Control-Boundary)**
Language: Java (Console-based, JDK 24)

---

## 📄 1. Module Overview

### 🏨 Front Desk Service
- **Room availability** — check whether a specific room is currently occupied or free
- **Billing details** — retrieve payment amount and status tied to a booking
- **Guest identification** — locate a guest's complete booking record using a unique **8-digit confirmation number**

---

## 💻 2. Abstract Data Type (ADT)

### 2.1 🌳 Binary Search Tree (applied in Front Desk Service)

1. binary search tree is **unambiguously non-linear** which is suitable for this module requirement without interpretation risk
2. Its ordering property (`left < parent < right`) doubles as a genuine **searching algorithm** — average-case **O(log n)** lookup, versus **O(n)** for a linear scan through hundreds of simultaneous bookings.
3. bookings are sorted by confirmation number for the tree, with no separate sort step will be required

#### 2.1.1 ADT Specification
| Operation | Signature | Description | Time Complexity |
|---|---|---|---|
| Insert | `void insert(K key, T data)` | Places a new key-data pair in correct sorted position | O(log n) avg, O(n) worst |
| Search | `T search(K key)` | Retrieves data by key, or `null` if not found | O(log n) avg, O(n) worst |
| Delete | `void delete(K key)` | Removes a node, re-linking via in-order successor/predecessor | O(log n) avg, O(n) worst |
| Is Empty | `boolean isEmpty()` | Checks whether the tree holds any data | O(1) |
| Size | `int size()` | Returns count of stored elements | O(1) |
| Traverse | `void inorderTraversal(BSTVisitor<T> visitor)` | Visits every node in ascending key order via callback | O(n) |

#### 2.1.2 Generic Design

```
Bst<K extends Comparable<K>, T>
Node<K, T>  →  key: K  |  data: T  |  left, right: Node<K,T>
```
 
- `K` = `String` (the 8-digit `confirmationNo`)
- `T` = `Booking` (the full guest/room/payment record)

searching only requires confirmation number typed from front-desk staff without the needs to construct 'Booking' object to compare

#### 2.1.3 Recursive & iterative

| Method | Style | Why |
|---|---|---|
| `insertHelper` | Recursive | Mirrors the tree's own recursive structure |
| `searchHelper` | Iterative (`while` + pointer) | Demonstrates equivalent logic without call-stack overhead |
| `deleteHelper` | Recursive | Handles 3 cases (leaf / one child / two children) via successor/predecessor substitution |
| `inorderHelper` | Recursive | Standard left → visit → right traversal |

---

## 📊 3. Reports

### Room Occupancy & Availability Report
- **Filters:** room type + available status
- **Sort:** room number ascending
- **Purpose:** allow staff or admin to have a review of occupied or available room by type

### Billing Summary Report
- **Filters:** payment status (Pending / Completed / Cancelled / Refunded)
- **Sort:** payment amount descending (selection sort)
- **Purpose:** revenue review, surfacing highest-value bookings and compute total revenue