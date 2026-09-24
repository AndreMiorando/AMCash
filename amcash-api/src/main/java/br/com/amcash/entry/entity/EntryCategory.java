package br.com.amcash.entry.entity;

public enum EntryCategory {
    FOOD(EntryType.EXPENSE),
    HOUSING(EntryType.EXPENSE),
    TRANSPORTATION(EntryType.EXPENSE),
    HEALTH(EntryType.EXPENSE),
    EDUCATION(EntryType.EXPENSE),
    CLOTHING(EntryType.EXPENSE),
    LEISURE(EntryType.EXPENSE),
    BILLS_AND_SERVICES(EntryType.EXPENSE),
    SUBSCRIPTIONS(EntryType.EXPENSE),
    CREDIT_CARD(EntryType.EXPENSE),
    LOANS_AND_FINANCING(EntryType.EXPENSE),
    FINANCIAL(EntryType.EXPENSE),
    INSURANCE(EntryType.EXPENSE),
    PERSONAL_CARE(EntryType.EXPENSE),
    TRAVEL(EntryType.EXPENSE),
    TAXES(EntryType.EXPENSE),
    FAMILY(EntryType.EXPENSE),
    PETS(EntryType.EXPENSE),
    SHOPPING(EntryType.EXPENSE),
    OTHER_EXPENSE(EntryType.EXPENSE),

    SALARY(EntryType.INCOME),
    FREELANCE(EntryType.INCOME),
    BUSINESS(EntryType.INCOME),
    INVESTMENT_INCOME(EntryType.INCOME),
    RENTAL_INCOME(EntryType.INCOME),
    BENEFITS(EntryType.INCOME),
    REFUND(EntryType.INCOME),
    GIFT(EntryType.INCOME),
    OTHER_INCOME(EntryType.INCOME);

    private final EntryType supportedType;

    EntryCategory(EntryType supportedType) {
        this.supportedType = supportedType;
    }

    public boolean supports(EntryType type) {
        return supportedType == type;
    }
}
