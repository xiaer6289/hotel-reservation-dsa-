# Hotel Reservation Prototype

Architecture: **ECB (Entity-Control-Boundary)**
Language: Java (Console-based, JDK 24)

---

## 📄 1. Module Overview

### 🏨 Front Desk Service
- **Room availability** — Check whether a specific room is currently occupied or free
- **Billing details** — Retrieve payment amount and status tied to a booking
- **Guest identification** — Locate a guest's complete booking record using a unique **8-digit confirmation number**

### 🚶 Walk-In Registrations & Standard Booking Procedure
- **Walk-in registration** — Register new or existing standard guests and create their walk-in registration records
- **FIFO waiting queue** — Manage standard guest registrations chronologically so the earliest waiting guest is processed first
- **Standard room assignment** — Assign a suitable ready room to the next eligible standard guest and complete the check-in process

### 👑 VIP & Loyalty Tier Priority Room Allocation
- **VIP priority allocation** — Prioritize eligible loyalty members for room assignment based on their membership tier
- **Loyalty tier ranking** — Process VIP guests according to **Diamond > Platinum > Elite**, with earlier registration time used when guests have the same tier
- **Priority room assignment** — Automatically reorganize waiting VIP registrations and assign suitable ready rooms according to priority

### 🧹 Housekeeping and Task Log
- **Cleaning status updates** — Manage room cleaning progress sequentially from **Dirty → Cleaning In Progress → Inspected → Ready for Check-In**
- **Housekeeping task log** — Record and track housekeeping tasks assigned to rooms and staff
- **Status rollback** — Reverse an incorrect housekeeping status when necessary to maintain accurate room conditions

---

## 💻 2. Abstract Data Type (ADT)

### 2.1 🌳 Binary Search Tree (applied in Front Desk Service)

1. Binary search tree is **unambiguously non-linear** which is suitable for this module requirement without interpretation risk.
2. Its ordering property (`left < parent < right`) doubles as a genuine **searching algorithm** — average-case **O(log n)** lookup, versus **O(n)** for a linear scan through hundreds of simultaneous bookings.
3. Bookings are sorted by confirmation number for the tree, with no separate sort step will be required.

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

Searching only requires confirmation number typed from front-desk staff without the needs to construct 'Booking' object to compare.

#### 2.1.3 Recursive & iterative

| Method | Style | Why |
|---|---|---|
| `insertHelper` | Recursive | Mirrors the tree's own recursive structure |
| `searchHelper` | Iterative (`while` + pointer) | Demonstrates equivalent logic without call-stack overhead |
| `deleteHelper` | Recursive | Handles 3 cases (leaf / one child / two children) via successor/predecessor substitution |
| `inorderHelper` | Recursive | Standard left → visit → right traversal |

### 2.2 🔗 Doubly Linked List (applied in Walk-In Registrations & Standard Booking Procedure and Housekeeping and Task Log)

1. Binary search tree is **unambiguously non-linear** which is suitable for this module requirement without interpretation risk.
2. Its ordering property (`left < parent < right`) doubles as a genuine **searching algorithm** — average-case **O(log n)** lookup, versus **O(n)** for a linear scan through hundreds of simultaneous bookings.
3. Bookings are sorted by confirmation number for the tree, with no separate sort step will be required.

#### 2.2.1 ADT Specification
| Operation | Signature | Description | Time Complexity |
|---|---|---|---|
| Add First | `void addFirst(T data)` | Places a new element at the beginning of the list | O(1) |
| Add Last | `void addLast(T data)` | Places a new element at the end of the list | O(1) |
| Add At | `boolean addAt(int index, T data)` | Inserts an element at a specified index | O(n) |
| Remove First | `T removeFirst()` | Removes and returns the first element | O(1) |
| Remove Last | `T removeLast()` | Removes and returns the last element | O(1) |
| Remove At | `T removeAt(int index)` | Removes and returns an element at a specified index | O(n) |
| Get | `T get(int index)` | Retrieves data stored at a specified index | O(n) |
| Contains | `boolean contains(T data)` | Checks whether the list contains specified data | O(n) |
| Index Of | `int indexOf(T data)` | Returns the index of specified data, or `-1` if not found | O(n) |
| Is Empty | `boolean isEmpty()` | Checks whether the list holds any data | O(1) |
| Size | `int size()` | Returns count of stored elements | O(1) |
| Clear | `void clear()` | Removes all elements from the list | O(1) |

#### 2.2.2 Generic Design

```
DoublyLinkedList<T>
Node<T> → data: T | previous: Node<T> | next: Node<T>
```
 
- `T` = `WalkInRegistration` (registration records stored in the standard FIFO waiting queue)
- `T` = `HousekeepingTask` (housekeeping task records stored sequentially)

The generic `<T>` allows the same doubly linked list implementation to store different entity objects without the needs to construct separate linked list implementations for each module.

#### 2.2.3 Sequential & iterative

| Method | Style | Why |
|---|---|---|
| `addFirst` | Direct pointer update | Inserts a node directly through `head` without traversal |
| `addLast` | Direct pointer update | Inserts a node directly through `tail`, suitable for adding new standard registrations to the rear of FIFO queue |
| `removeFirst` | Direct pointer update | Removes the earliest registration through `head` in O(1) |
| `removeLast` | Direct pointer update | Removes the final node directly through `tail` and its previous link |
| `get` / `removeAt` | Direct pointer update | Removes the earliest registration through `head` in O(1) |
| `contains` / `indexOf` | Direct pointer update | Removes the final node directly through `tail` and its previous link |

### 2.3 👑 MaxHeap (applied in VIP & Loyalty Tier Priority Room Allocation)

1. Max heap is **unambiguously non-linear** which is suitable for VIP room allocation where guests are processed according to loyalty priority rather than normal chronological FIFO order.
2. Its heap property keeps the highest-priority registration at the root, providing **O(1)** access to the next priority guest while insertion and removal require **O(log n)** reorganisation.
3. VIP registrations are ordered according to membership tier (`DIAMOND > PLATINUM > ELITE`), while earlier registration time is used as the tie-breaker when two guests have the same tier, allowing the structure to automatically reorganise whenever a new VIP registration is inserted.

#### 2.3.1 ADT Specification
| Operation | Signature | Description | Time Complexity |
|---|---|---|---|
| Enqueue | `void enqueue(T data)` | Inserts a new element and restores MaxHeap priority order | O(log n) |
| Peek | `T peek()` | Retrieves the highest-priority element without removing it | O(1) |
| Dequeue | `T dequeue()` | Removes the highest-priority element and restores heap order | O(log n) |
| Remove | `boolean remove(T data)` | Searches for and removes a specified element from the heap | O(n) |
| Is Empty | `boolean isEmpty()` | Checks whether the heap holds any data | O(1) |
| Size | `int size()` | Returns count of stored elements | O(1) |
| Clear | `void clear()` | Removes all elements from the heap | O(n) |

#### 2.3.2 Generic Design

```
MaxHeap<T>
PriorityQueueADT<T>
        ↑
    MaxHeap<T>
```
 
- `T` = `WalkInRegistration` (VIP and loyalty waiting registration)
- `Comparator<T>` = determines VIP priority according to loyalty tier and registration time

The generic `<T>` allows the MaxHeap to manage entity objects independently from their business priority rules, while the comparator determines which `WalkInRegistration` should be positioned ahead of another without the needs to hard-code guest data inside the ADT.

#### 2.3.3 Reheap-up & reheap-down

| Method | Style | Why |
|---|---|---|
| `enqueue` | Iterative reheap-up | Inserts at the next available position and moves the registration upward while its priority is higher than its parent |
| `dequeue` | Iterative reheap-down | Removes the root and moves the replacement downward until MaxHeap priority is restored |
| `peek` | Direct access | Retrieves the root because the highest-priority registration is maintained at the front of the heap |
| `remove` | Linear search + reheap | Searches for a specific registration and restores heap order after removal |
| `compareVipPriority` | Comparator-based | Compares loyalty tier first and uses registration time as the tie-breaker |

---

## 📊 3. Reports

### Room Occupancy & Availability Report
- **Filters:** Room type + Available status
- **Sort:** Room number ascending
- **Purpose:** Allow staff or admin to have a review of occupied or available room by type

### Billing Summary Report
- **Filters:** Payment status (Pending / Completed / Cancelled / Refunded)
- **Sort:** Payment amount descending (selection sort)
- **Purpose:** Revenue review, surfacing highest-value bookings and compute total revenue

### Standard FIFO Waiting Time Analysis Report
- **Filters:** Registration ID / Guest ID / Guest name + Room type + Minimum party size + Minimum waiting time
- **Sort:** FIFO earliest arrival / Longest waiting time / Largest party size (selection sort)
- **Purpose:** Analyze the current standard waiting queue, identify long-waiting guests and support FIFO room allocation decisions

### Walk-In Arrival Pattern Analysis Report
- **Filters:** Date range + Room type + Minimum party size
- **Sort:** Earliest arrival / Latest arrival / Largest party size (selection sort)
- **Purpose:** Analyze walk-in arrival patterns, guest demand and peak arrival periods for room and staffing planning

### VIP Priority Allocation Performance Report
- **Filters:** Member ID / Registration ID / Guest ID / Guest name + Loyalty tier + Requested room type + Minimum guests
- **Sort:** Loyalty tier descending + Arrival time ascending (selection sort)
- **Purpose:** Analyze VIP allocation priority and waiting performance to ensure higher-tier loyalty guests are served according to the required priority rules

### VIP Loyalty & Stay Performance Report
- **Filters:** Registration ID / Guest ID / Guest name + Loyalty tier + Room type + Minimum waiting time + Minimum party size
- **Sort:** Loyalty tier descending + Arrival time ascending (selection sort)
- **Purpose:** Analyze VIP guest activity by loyalty tier, room preference and stay-related performance for management review

### Cleaning Status Flow Report
- **Filters:** Cleaning status + Staff + Room number
- **Sort:** Room number / Task update time (selection sort)
- **Purpose:** Review the cleaning progress of hotel rooms and identify rooms that are still being cleaned, inspected or ready for check-in

### Daily Performance Report
- **Filters:** Date + Staff + Minimum task time
- **Sort:** Task created time / Task completion time (selection sort)
- **Purpose:** Analyze daily housekeeping workload and staff task performance to support housekeeping supervision and operational planning