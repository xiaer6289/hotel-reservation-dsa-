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
- **Walk-in registration** — Register new or existing guests and create Standard or VIP walk-in registration records
- **FIFO waiting queue** — Manage Standard guest registrations chronologically so that the earliest waiting Standard guest is processed first
- **Waiting-time monitoring** — Display each Standard guest's arrival/request time and current waiting time in minutes or hours
- **Standard room assignment and check-in** — Assign a suitable ready room to the next eligible Standard guest and complete the check-in process
- **Registration analysis** — Generate Standard FIFO Waiting Time and Walk-In Arrival Pattern reports for operational decision-making

### 👑 VIP & Loyalty Tier Priority Room Allocation
- **VIP priority allocation** — Prioritize eligible loyalty members for room assignment according to loyalty tier
- **Loyalty tier ranking** — Process VIP guests according to **Diamond > Platinum > Elite**, with earlier request time used as the tie-breaker for guests in the same tier
- **MaxHeap priority queue** — Automatically reorganize VIP waiting registrations whenever a VIP request is inserted, updated, cancelled, or allocated
- **VIP waiting-time monitoring** — Display request time and waiting time using readable minute/hour wording
- **VIP room assignment and check-in** — Assign suitable ready rooms to the highest-priority eligible VIP guest
- **VIP analysis** — Generate VIP Room Allocation & Waiting Time and VIP Loyalty Engagement reports for management review

### 🧹 Housekeeping and Task Log
- **FIFO dirty-room queue** — Add dirty rooms to a Linear ADT and process them according to first-in-first-out order
- **Automatic staff assignment** — Automatically assign the next dirty room to an available housekeeping staff member
- **Cleaning status updates** — Manage room cleaning progress sequentially from **Dirty → Cleaning In Progress → Inspected → Ready for Check-In**
- **Cleaning-time monitoring** — Track the 30-minute cleaning target and allow staff to mark an early completion
- **Housekeeping task log** — Record and track room-cleaning tasks, assigned staff, timestamps, and completion information
- **Status rollback** — Reverse an incorrect housekeeping status when necessary while maintaining the dirty-room queue
- **KPI analysis** — Evaluate housekeeping staff using daily room-cleaning and cleaning-time targets
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

1. Doubly Linked List is a **Linear ADT** that stores elements sequentially through nodes containing references to both the previous and next nodes.

2. It is suitable for the Standard Registration module because Standard guests must be processed chronologically using **FIFO order**. New Standard registrations can be added at the rear using `addLast()`, while the earliest waiting registration can be accessed or removed from the front.

3. It is also suitable for Housekeeping because dirty rooms and housekeeping tasks must be processed sequentially. Dirty rooms can enter a FIFO queue and be dispatched to available staff according to queue order.

#### 2.2.1 ADT Specification

| Operation | Signature | Description | Time Complexity |
|---|---|---|---|
| Add First | `void addFirst(T data)` | Adds an element at the beginning of the list | O(1) |
| Add Last | `void addLast(T data)` | Adds an element at the end of the list | O(1) |
| Add At | `boolean addAt(int index, T data)` | Inserts an element at a specified index | O(n) |
| Remove First | `T removeFirst()` | Removes and returns the first element | O(1) |
| Remove Last | `T removeLast()` | Removes and returns the last element | O(1) |
| Remove At | `T removeAt(int index)` | Removes and returns an element at a specified index | O(n) |
| Get | `T get(int index)` | Retrieves an element at a specified index | O(n) |
| Contains | `boolean contains(T data)` | Checks whether a specified element exists in the list | O(n) |
| Index Of | `int indexOf(T data)` | Returns the index of a specified element, or `-1` when not found | O(n) |
| Is Empty | `boolean isEmpty()` | Checks whether the list contains any elements | O(1) |
| Size | `int size()` | Returns the number of elements | O(1) |
| Clear | `void clear()` | Removes all elements from the list | O(1) |

#### 2.2.2 Generic Design

```text
DoublyLinkedList<T>

Node<T>
→ data: T
→ previous: Node<T>
→ next: Node<T>
```

### 2.3 👑 MaxHeap (applied in VIP & Loyalty Tier Priority Room Allocation)

1. Max heap is **unambiguously non-linear** which is suitable for VIP room allocation where guests are processed according to loyalty priority rather than normal chronological FIFO order.
2. Its heap property keeps the highest-priority registration at the root, providing **O(1)** access to the next priority guest while insertion and removal require **O(log n)** reorganisation.
3. VIP registrations are ordered according to loyalty tier (`DIAMOND > PLATINUM > ELITE`). When two VIP guests have the same tier, the earlier request time is used as the tie-breaker. This allows the MaxHeap to automatically reorganize whenever a VIP registration is inserted, updated, removed, or allocated.

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
| `compareVipPriority` | Comparator-based | Compares loyalty tier first and uses earlier request/registration time as the tie-breaker |
---

## 📊 3. Reports

### Room Occupancy & Availability Report
- **Filters:** Room type + data range based on check in date time
- **Sort:** Top-utilized rooms ranked by occupied room-days descending (selection sort)
- **Purpose:** provide clear picture of day-by-day occupancy of the rooms to support pricing, housekeeping prioritization, and marketing decisions across based on month (season)

### Billing Summary Report
- **Filters:** Payment status (Pending / Completed / Cancelled / Refunded)
- **Sort:** Top-value bookings ranked by payment amount descending (selection sort)
- **Purpose:** revenue review to check where the revenue comes from and how healthy is the collections

### Standard FIFO Waiting Time Analysis Report
- **Filters:** Registration ID / Guest ID / Guest name + Room type + Minimum party size + Minimum waiting time
- **Sort:** FIFO earliest arrival / Largest party size (Selection Sort)
- **Purpose:** Analyze the current Standard FIFO waiting queue, identify long-waiting guests, monitor average and longest waiting times, and support room-assignment decisions while preserving chronological FIFO order

### Walk-In Arrival Pattern Analysis Report
- **Filters:** Start date + End date
- **Sort:** Not applicable — registrations are grouped into predefined arrival-time periods for analytical comparison
- **Purpose:** Analyze walk-in arrival patterns by time period, identify peak arrival periods, measure guest demand and average party size, and support front-desk staffing and room-readiness planning

### VIP Room Allocation & Waiting Time Report
- **Filters:** Registration / Guest keyword + Loyalty tier + Requested room type + VIP registration status + Registration date range + Minimum party size
- **Sort:** VIP tier priority then earliest request / Longest waiting time / Latest registration / Requested room type A-Z (Selection Sort)
- **Purpose:** Monitor VIP waiting times and evaluate the effectiveness of priority-based room allocation, allowing management to identify long VIP waits, room shortages and allocation bottlenecks while confirming that higher-tier guests receive appropriate priority

### VIP Loyalty Engagement Report
- **Filters:** Guest keyword + Loyalty tier + Current VIP activity + Minimum completed stays + Stay room type + Check-in date range
- **Sort:** Loyalty tier priority / Completed stays highest first / Most recent stay latest first / Guest name A-Z / Closest to next loyalty tier (Selection Sort)
- **Purpose:** Identify valuable repeat VIP guests, monitor loyalty-tier activity and progression, recognise guests close to the next tier, and support personalised retention and promotion decisions

### Cleaning Status Analysis Report
- **Filters:** Cleaning status + Housekeeping staff + Room number
- **Sort:** Room number ascending / Time spent longest first (Selection Sort)
- **Purpose:** Review the current cleaning progress of hotel rooms, identify rooms or tasks requiring attention, and help supervisors monitor cleaning workload and operational bottlenecks

### Daily Housekeeping Performance Report
- **Filters:** Report date + Housekeeping staff + Minimum task time
- **Sort:** Task created time earliest first / Task time longest first (Selection Sort)
- **Purpose:** Analyze daily housekeeping workload and cleaning-task performance, allowing supervisors to review staff activity, identify unusually long tasks and support day-to-day manpower planning

### Housekeeping KPI Report
- **Filters:** Report date
- **Sort:** Rooms cleaned per staff, highest first (Selection Sort)
- **Purpose:** Evaluate daily housekeeping staff productivity using defined KPI targets, identify staff who did not meet the required performance level, and support workload review or additional staff support
- **KPI Target:** At least 5 rooms cleaned per staff per day
- **Time Target:** Each completed room should be cleaned within 30 minutes