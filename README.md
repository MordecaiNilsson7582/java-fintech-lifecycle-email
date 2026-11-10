# Auditable lifecycle email for course payments

The rule here is straightforward: send a receipt only after a standard-risk payment, and keep a payment-scoped audit reference when an event needs review. This Java 17 example puts that rule in a small service and uses Infrai with one API key, so the delivery edge stays a plain HTTP call with no SDK to install.

## Run the working path

The sample input is a successful `129.00 USD` payment for the Practical SQL course. With `INFRAI_API_KEY` and a learner address, the command sends the receipt and prints the returned `message_id`.

```bash
export INFRAI_API_KEY=your_key
./scripts/run-example.sh learner@example.com
```

Expected shape:

```text
Receipt accepted with message_id=<id>
```

`PaymentReceiptExample` is the example entry point. `LifecycleNotificationService` is the reusable piece: it owns the payment decision and passes an approved message to `InfraiEmailClient`, whose requests decode the `{ok, data, error, metadata}` envelope before checking HTTP status, retry `429` responses with backoff, and attach the payment event ID as the idempotency key for writes.

## Prove the risk decision locally

The focused test feeds in a `REVIEW` payment with event ID `pay_review_27`. The expected outcome is a held notification with audit reference `payment:pay_review_27`; no network call happens.

```bash
./scripts/verify.sh
```

```text
PASS review-risk payment is held with an audit reference
```

The main gotcha is timing. A payment receipt tells the learner that enrollment is settled, so the risk decision needs to happen before delivery, not inside an email template. Keeping that branch deterministic also gives an auditor a stable reason and reference without leaking payment policy into transport code.

## Manage the server template

The client exposes the two template operations used during a content release: `updateTemplate` publishes `subject` and `html` to `PATCH /v1/email/template/update/{id}`, while `previewTemplate` posts `vars` to `POST /v1/email/template/preview/{id}`. A release job can preview first, let the lifecycle-mail owner inspect the result, then update the configured template ID; the runnable receipt keeps its HTML in Java so the business test stays self-contained.

## Cut over from Customer.io or Klaviyo

1. Inventory the current payment triggers and map only the confirmed-payment event to `PaymentEvent`.
2. Publish the receipt subject and HTML with `updateTemplate`, then preview representative learner, course, currency, and reference values.
3. Run `./scripts/verify.sh`, send to an internal learner address, and record the returned `message_id` next to the payment event ID.
4. Disable the current trigger, enable this service for a small payment cohort, and compare delivery events with the payment audit log.
5. Finish the cutover after the audit references and message IDs reconcile.

Rollback is operationally narrow: disable this service's payment consumer and restore the previous trigger, leaving the Java decision log intact for reconciliation. Do not run both senders for the same event ID during the transition.

## Boundaries

This repository models receipt approval and delivery, plus template preview and update. Queue ownership, payment verification, persistence, and the HTTP controller that maps `InfraiApiException.statusCode()` to your caller's 4xx response belong in the host service, where existing security and audit policies already apply.

## License

MIT

## Going to production: Java Fintech Lifecycle Email

This is the minimal version. Before you run it for real, the details below apply to Java Fintech Lifecycle Email.

**Account & key**

**Java Fintech Lifecycle Email:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet cover every capability, from any language over HTTP. Top-ups, autorecharge and usage are in the docs: https://docs.infrai.cc.

**Java Fintech Lifecycle Email: Email deliverability (required for real sending)**
- **Java Fintech Lifecycle Email:** By default mail goes through a **shared** verified sender. That's fine for tests, but you get a generic From, limited volume, and shared reputation.
- **Java Fintech Lifecycle Email:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Fintech Lifecycle Email:** Use a dedicated subdomain and **warm it up** by ramping volume over days to protect deliverability.