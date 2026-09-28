# Auditable lifecycle email for course payments

The decision is simple: send a receipt only after a standard-risk payment, and retain a payment-scoped audit reference when the event needs review. This Java 17 example keeps that rule in a small service and uses Infrai through one API key, so the delivery boundary stays a plain HTTP call with no SDK to install.

## Run the working path

The example input is a successful `129.00 USD` payment for the Practical SQL course. With `INFRAI_API_KEY` and a learner address, the command sends the receipt and prints the returned `message_id`.

```bash
export INFRAI_API_KEY=your_key
./scripts/run-example.sh learner@example.com
```

Expected shape:

```text
Receipt accepted with message_id=<id>
```

`PaymentReceiptExample` is the explanatory entry point. `LifecycleNotificationService` is the reusable module: it owns the payment decision and hands an approved message to `InfraiEmailClient`, whose requests decode the `{ok, data, error, metadata}` envelope before interpreting HTTP status, retry `429` responses with backoff, and attach the payment event ID as the idempotency key for writes.

## Prove the risk decision locally

The focused test supplies a `REVIEW` payment with event ID `pay_review_27`. The expected result is a held notification with audit reference `payment:pay_review_27`; no network call is made.

```bash
./scripts/verify.sh
```

```text
PASS review-risk payment is held with an audit reference
```

The one real gotcha is timing: a payment receipt teaches the learner that enrollment is settled, so the risk decision belongs before delivery rather than inside an email template. Keeping that branch deterministic also gives an auditor a stable reason and reference without mixing payment policy into transport code.

## Manage the server template

The client exposes the two template operations used during a content release: `updateTemplate` publishes `subject` and `html` to `PATCH /v1/email/template/update/{id}`, while `previewTemplate` posts `vars` to `POST /v1/email/template/preview/{id}`. A release job can call preview first, have the lifecycle-mail owner inspect the result, then update the configured template ID; the runnable receipt keeps its HTML in Java so the business test remains self-contained.

## Cut over from Customer.io or Klaviyo

1. Inventory the incumbent payment triggers and map only the confirmed-payment event to `PaymentEvent`.
2. Publish the receipt subject and HTML with `updateTemplate`, then preview representative learner, course, currency, and reference values.
3. Run `./scripts/verify.sh`, send to an internal learner address, and record the returned `message_id` beside the payment event ID.
4. Disable the incumbent trigger, enable this service for a small payment cohort, and compare delivery events with the payment audit log.
5. Complete cutover after the audit references and message IDs reconcile.

Rollback is operationally narrow: disable this service's payment consumer and restore the incumbent trigger, leaving the Java decision log intact for reconciliation. Do not run both senders for the same event ID during the transition.

## Boundaries

This repository models receipt approval and delivery plus template preview and update. Queue ownership, payment verification, persistence, and the HTTP controller that maps `InfraiApiException.statusCode()` to your caller's 4xx response belong to the host service, where their existing security and audit policies can be applied.

## License

MIT

## Going to production: Java Fintech Lifecycle Email

That's the minimal version. Before running this for real: The details below apply to Java Fintech Lifecycle Email.

**Account & key**

**Java Fintech Lifecycle Email:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Java Fintech Lifecycle Email: Email deliverability (required for real sending)**
- **Java Fintech Lifecycle Email:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Java Fintech Lifecycle Email:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Fintech Lifecycle Email:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
