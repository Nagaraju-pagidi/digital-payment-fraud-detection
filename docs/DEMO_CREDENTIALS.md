# Demo credentials & test data

> Keep this document private. Nothing below is shown anywhere in the website — just like a real bank,
> customers must type their own User ID, password and PIN.

## 1. Ready-made bank customers (NexaPay — http://localhost:5173)

Every customer has a **different** password and transaction PIN.

| Customer | User ID | Password | Transaction PIN | Customer ID | Account number | IFSC | UPI ID |
|---|---|---|---|---|---|---|---|
| Ravi Kumar | `ravi.kumar` | `Ravi@2026` | `482915` | FP1000001 | 501012345671 | FRPY0HYD001 | ravi.kumar@fraudpay |
| Priya Sharma | `priya.sharma` | `Priya#2026` | `736204` | FP1000002 | 501012345682 | FRPY0BEN001 | priya.sharma@fraudpay |
| Arjun Mehta | `arjun.mehta` | `Arjun$2026` | `915372` | FP1000003 | 501012345693 | FRPY0PUN001 | arjun.mehta@fraudpay |
| Sneha Patel | `sneha.patel` | `Sneha&2026` | `604183` | FP1000004 | 501012345704 | FRPY0MUM001 | sneha.patel@fraudpay |

Who they are (this is what FraudShield "knows" about their normal behaviour):

| Customer | Story | Saved payees |
|---|---|---|
| Ravi | Salaried, account 2 years old, usually pays about ₹1,800 | Priya, Sneha |
| Priya | Salaried, usually pays about ₹2,500 | Ravi, Sneha |
| Arjun | **Student, account opened 2 days ago, no history** | none |
| Sneha | Business owner (current account), usually pays about ₹6,000 | Ravi, Priya |

**First login of each demo customer** registers *that browser as the trusted device* and *your current
location as the home location* (like setting up a bank app for the first time). From then on:
* the same browser logs in without an OTP;
* another browser / Incognito window / phone = **new device** → OTP by email.

## 2. Emails and OTPs

All emails (OTPs, alerts, welcome mail) go to the fake inbox **Mailpit: http://localhost:8025**.
Open it in another tab during the demo. OTPs are 6 digits, valid 5 minutes, 3 tries, single use.

When is an OTP asked?
* Opening an account (email verification)
* Login from a new device
* Adding a payee
* Every IMPS / NEFT transfer (UPI uses only the transaction PIN, like real UPI)

## 3. FraudShield Control Center (http://localhost:5174)

| User | Password |
|---|---|
| `analyst` | `password123` |

## 4. Test data for opening a NEW account

The account-opening form validates like a real bank, so use data that passes the checks:

| Field | Rule | Example that works |
|---|---|---|
| Full name | letters and spaces, first name + surname | `Kiran Rao` |
| Date of birth | 18 to 100 years old | `1999-04-10` |
| Mobile | 10 digits, starts with 6-9, not already used | `9876512345` |
| Email | valid and not already used | `kiran.rao@demo.in` |
| PAN | `AAAPS9999A`: 4th letter **P** (person), **5th letter = first letter of surname** | for *Kiran **R**ao* → `ABCPR1234Z` |
| Aadhaar | 12 digits, not starting with 0/1, valid **Verhoeff checksum** (same check UIDAI uses) | `281729306027`, `392830417034`, `482910374653`, `593021485762` |
| PIN code | 6 digits, not starting with 0 | `500072` |
| User ID | 5-20 chars, starts with a letter | `kiran.rao` |
| Password | 8-20 chars, upper + lower + digit + special, must not contain the User ID | `Kiran@2026` |
| Transaction PIN | 6 digits, not `111111` / `123456` / birth date / part of mobile | `730418` |

(The Aadhaar numbers above are made-up numbers that only pass the checksum — they belong to no one.
Each can be used for one account only. More can be generated with any "Verhoeff check digit" tool.)

New accounts get ₹50,000 of demo money, a Customer ID (FP…), a 12-digit account number, an IFSC based on the
city and a UPI ID `<userid>@fraudpay`.

## 5. Security limits you can demonstrate

| Rule | What happens |
|---|---|
| 5 wrong passwords | login locked for 15 minutes + email alert |
| 3 wrong transaction PINs | payments locked for 30 minutes + email alert |
| New device | email OTP before login; can be "remembered" |
| New payee | email OTP, then a **2-minute cooling period** before money can be sent |
| 5 minutes without activity | automatic logout (warning 60 s before) |
| Session token | expires after 30 minutes; closing the tab ends the session |
| Limits (bank rules) | IMPS ₹5 lakh, NEFT ₹10 lakh per transfer, ₹10 lakh/day net banking; UPI ₹1 lakh per payment and per day |

> Tip: if you lock a demo customer by mistake, wait 15/30 minutes or reset everything with
> `docker compose down -v` followed by `docker compose up -d`.
