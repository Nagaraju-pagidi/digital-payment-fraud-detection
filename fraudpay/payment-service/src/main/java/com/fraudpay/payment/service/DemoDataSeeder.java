package com.fraudpay.payment.service;

import com.fraudpay.payment.domain.AccountType;
import com.fraudpay.payment.domain.Beneficiary;
import com.fraudpay.payment.domain.LedgerEntry;
import com.fraudpay.payment.domain.User;
import com.fraudpay.payment.geo.GeoCatalog;
import com.fraudpay.payment.repository.BeneficiaryRepository;
import com.fraudpay.payment.repository.LedgerRepository;
import com.fraudpay.payment.repository.UserRepository;
import com.fraudpay.payment.security.KycValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates demo customers on the very first start (empty database).
 * IDs 1-4 are demo customers, IDs 5-10 are simulator bots (cannot log in).
 * FraudShield's database has matching "normal behaviour" profiles for these IDs.
 *
 * Login details are NOT shown anywhere in the website - see docs/DEMO_CREDENTIALS.md.
 * Each customer has a different password and transaction PIN.
 * Device and "home" location are registered at the customer's first login.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** amountRupees > 0 = credit, < 0 = debit */
    private record Txn(int daysAgo, long amountRupees, String description) {
    }

    private record Seed(String name, LocalDate dob, String gender, String email, String phone, String pan,
                        String aadhaar, String address, String city, String pincode, String occupation,
                        String income, AccountType type, String nominee, String relation, String username,
                        String password, String pin, String customerId, String accountNumber, long balance,
                        int ageDays, boolean bot, List<Txn> history) {
    }

    private static final List<Seed> SEEDS = List.of(
            new Seed("Ravi Kumar", LocalDate.of(1994, 6, 12), "MALE", "ravi.kumar@demo.in", "9876500001",
                    "BQRPK4821L", "471829365028", "Flat 302, Sai Residency, Madhapur", "Hyderabad", "500081",
                    "SALARIED", "5L-10L", AccountType.SAVINGS, "Lakshmi Kumar", "SPOUSE",
                    "ravi.kumar", "Ravi@2026", "482915", "FP1000001", "501012345671", 85000, 730, false,
                    List.of(new Txn(40, 30000, "Salary credit - Infotech Solutions Pvt Ltd"),
                            new Txn(38, -1800, "UPI to Priya Sharma - dinner"),
                            new Txn(31, -2200, "IMPS to Sneha Patel - books"),
                            new Txn(25, -1500, "UPI to Priya Sharma - movie tickets"),
                            new Txn(10, 30000, "Salary credit - Infotech Solutions Pvt Ltd"),
                            new Txn(6, -1900, "IMPS to Sneha Patel"))),
            new Seed("Priya Sharma", LocalDate.of(1997, 3, 25), "FEMALE", "priya.sharma@demo.in", "9876500002",
                    "CKTPS5532M", "582739416036", "No. 14, 2nd Cross, Indiranagar", "Bengaluru", "560038",
                    "SALARIED", "10L-25L", AccountType.SAVINGS, "Meena Sharma", "MOTHER",
                    "priya.sharma", "Priya#2026", "736204", "FP1000002", "501012345682", 62000, 400, false,
                    List.of(new Txn(35, 45000, "Salary credit - Bright Analytics"),
                            new Txn(30, -2500, "UPI to Ravi Kumar"),
                            new Txn(21, -2700, "IMPS to Sneha Patel - rent share"),
                            new Txn(5, -2400, "UPI to Ravi Kumar"))),
            new Seed("Arjun Mehta", LocalDate.of(2003, 11, 2), "MALE", "arjun.mehta@demo.in", "9876500003",
                    "DLWPM7714Q", "693841527040", "Room 12, Shanti Hostel, Kothrud", "Pune", "411038",
                    "STUDENT", "BELOW_2.5L", AccountType.SAVINGS, null, null,
                    "arjun.mehta", "Arjun$2026", "915372", "FP1000003", "501012345693", 40000, 2, false,
                    List.of()),
            new Seed("Sneha Patel", LocalDate.of(1990, 1, 8), "FEMALE", "sneha.patel@demo.in", "9876500004",
                    "EMZPP9021R", "734952638055", "Shop 7, Laxmi Market, Andheri West", "Mumbai", "400058",
                    "BUSINESS", "ABOVE_25L", AccountType.CURRENT, "Raj Patel", "SPOUSE",
                    "sneha.patel", "Sneha&2026", "604183", "FP1000004", "501012345704", 150000, 1100, false,
                    List.of(new Txn(45, 120000, "NEFT from Patel Traders - business receipts"),
                            new Txn(33, -6000, "IMPS to Ravi Kumar"),
                            new Txn(20, -5500, "IMPS to Priya Sharma"),
                            new Txn(8, -6500, "UPI to Ravi Kumar"))),
            bot("Amit Das", "amit.bot", "825163749062", "Kolkata", "700001", "FP9000005", "501090000005", "FDNPD1001A"),
            bot("Divya Rathore", "divya.bot", "936274851071", "Jaipur", "302001", "FP9000006", "501090000006", "GHJPR1002B"),
            bot("Karthik Iyer", "karthik.bot", "647385962087", "Chennai", "600001", "FP9000007", "501090000007", "HJKPI1003C"),
            bot("Meera Nair", "meera.bot", "358496073091", "Kochi", "682001", "FP9000008", "501090000008", "JKLPN1004D"),
            bot("Rahul Verma", "rahul.bot", "469507184007", "Delhi", "110001", "FP9000009", "501090000009", "KLMPV1005E"),
            bot("Fatima Khan", "fatima.bot", "570618295014", "Lucknow", "226001", "FP9000010", "501090000010", "LMNPK1006F"));

    private static Seed bot(String name, String username, String aadhaar, String city, String pin, String customerId,
                            String account, String pan) {
        return new Seed(name, LocalDate.of(1992, 5, 5), "OTHER", username + "@demo.in",
                "91000000" + account.substring(10), pan, aadhaar, "Simulator account, " + city, city, pin,
                "OTHER", "5L-10L", AccountType.SAVINGS, null, null, username, "Bot#Locked2026", "739184",
                customerId, account, 60000, 500, true, List.of());
    }

    private final UserRepository users;
    private final LedgerRepository ledger;
    private final BeneficiaryRepository payees;
    private final PasswordEncoder encoder;
    private final GeoCatalog geo;
    private final AuthService auth;

    public DemoDataSeeder(UserRepository users, LedgerRepository ledger, BeneficiaryRepository payees,
                          PasswordEncoder encoder, GeoCatalog geo, AuthService auth) {
        this.users = users;
        this.ledger = ledger;
        this.payees = payees;
        this.encoder = encoder;
        this.geo = geo;
        this.auth = auth;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;

        List<User> created = new ArrayList<>();
        for (Seed s : SEEDS) {
            GeoCatalog.City city = geo.findHomeCity(s.city()).orElseThrow();
            User u = new User();
            u.setCustomerId(s.customerId());
            u.setUsername(s.username());
            u.setFullName(s.name());
            u.setDateOfBirth(s.dob());
            u.setGender(s.gender());
            u.setEmail(s.email());
            u.setPhone(s.phone());
            u.setPan(s.pan());
            u.setAadhaarHash(KycValidator.sha256("FRAUDPAY-AADHAAR:" + s.aadhaar()));
            u.setAadhaarLast4(s.aadhaar().substring(8));
            u.setAddressLine(s.address());
            u.setCity(city.name());
            u.setState(city.state());
            u.setPincode(s.pincode());
            u.setOccupation(s.occupation());
            u.setAnnualIncome(s.income());
            u.setNomineeName(s.nominee());
            u.setNomineeRelation(s.relation());
            u.setAccountType(s.type());
            u.setAccountNumber(s.accountNumber());
            u.setIfsc(auth.ifscFor(city.name()));
            u.setBranch(city.name() + " Main Branch");
            u.setUpiId(s.username() + "@fraudpay");
            u.setPasswordHash(encoder.encode(s.password()));
            u.setPinHash(encoder.encode(s.pin()));
            u.setBalance(BigDecimal.valueOf(s.balance()));
            u.setCreatedAt(Instant.now().minus(Duration.ofDays(s.ageDays())));
            u.setBot(s.bot());
            if (s.bot()) {
                // bots always pay from their home city with a fixed device
                u.setHomeCity(city.name());
                u.setHomeCountry(city.country());
                u.setHomeLat(city.lat());
                u.setHomeLon(city.lon());
                u.setRegisteredDeviceId("bot-device-" + s.username());
            }
            users.saveAndFlush(u);
            created.add(u);
            seedStatement(u, s);
        }

        // Saved payees (added long ago, so they are active)
        payee(created.get(0), created.get(1));
        payee(created.get(0), created.get(3));
        payee(created.get(1), created.get(0));
        payee(created.get(1), created.get(3));
        payee(created.get(3), created.get(0));
        payee(created.get(3), created.get(1));

        log.info("Seeded {} demo accounts - login details are in docs/DEMO_CREDENTIALS.md", SEEDS.size());
    }

    /** Builds a realistic passbook that ends exactly at the seeded balance. */
    private void seedStatement(User u, Seed s) {
        long net = s.history().stream().mapToLong(Txn::amountRupees).sum();
        long opening = s.balance() - net;
        long running = opening;
        Instant openedAt = u.getCreatedAt().isBefore(Instant.now().minus(Duration.ofDays(60)))
                ? Instant.now().minus(Duration.ofDays(60)) : u.getCreatedAt();
        LedgerEntry first = new LedgerEntry(u.getId(), null, "OPENING", BigDecimal.valueOf(opening),
                BigDecimal.valueOf(opening), s.history().isEmpty() ? "Account opened - initial deposit" : "Opening balance");
        first.setCreatedAt(openedAt);
        ledger.save(first);
        for (Txn t : s.history()) {
            running += t.amountRupees();
            LedgerEntry e = new LedgerEntry(u.getId(), null, t.amountRupees() > 0 ? "CREDIT" : "DEBIT",
                    BigDecimal.valueOf(Math.abs(t.amountRupees())), BigDecimal.valueOf(running), t.description());
            e.setCreatedAt(Instant.now().minus(Duration.ofDays(t.daysAgo())));
            ledger.save(e);
        }
    }

    private void payee(User owner, User target) {
        Beneficiary b = new Beneficiary(owner.getId(), target.getId(), target.getFullName(),
                target.getFullName().split(" ")[0], target.getAccountNumber(), target.getIfsc(), Instant.now().minus(Duration.ofDays(90)));
        b.setCreatedAt(Instant.now().minus(Duration.ofDays(90)));
        payees.save(b);
    }
}
