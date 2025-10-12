# ACID




ACID properties are a set of principles that ensure reliable and consistent database transactions, especially in relational database management systems (RDBMS).

The acronym stands for **Atomicity**, **Consistency**, **Isolation**, and **Durability**.  

These properties collectively guarantee that database transactions are processed correctly even in the presence of errors, failures, or concurrent operations.

### Atomicity
Atomicity ensures that a transaction is treated as a single, indivisible unit of work.  
Either all operations within the transaction are completed successfully (committed), or none of them are applied (rolled back). This prevents partial updates that could leave the database in an inconsistent state. For example, in a bank transfer, debiting one account and crediting another must both succeed or both fail.

### Consistency
Consistency guarantees that a transaction brings the database from one valid state to another, adhering to all defined rules, constraints, triggers, and data integrity checks (e.g., primary keys, foreign keys, or custom business logic). If a transaction violates any consistency rules, it is rolled back. This property maintains the overall integrity of the data across the database.

### Isolation
Isolation ensures that transactions are executed independently of one another. Concurrent transactions do not interfere with each other, preventing issues like dirty reads (reading uncommitted data), non-repeatable reads, or phantom reads. Isolation levels (e.g., read uncommitted, read committed, repeatable read, serializable) can be adjusted to balance consistency with performance.

### Durability
Durability means that once a transaction is committed, its changes are permanently saved to the database, even in the event of a system failure like a power outage. This is typically achieved through techniques like write-ahead logging (WAL) or persistent storage to non-volatile media.

<br>

ACID compliance is crucial for applications requiring high reliability, such as financial systems, but it can introduce overhead in terms of performance. Modern databases often provide configurable ACID support, and NoSQL databases may prioritize scalability over full ACID compliance (e.g., via BASE properties: Basically Available, Soft state, Eventual consistency).
