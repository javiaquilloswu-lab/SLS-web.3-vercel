# Student Life Services — PHP REST API

**Student Life Services** is a student-services system developed for **Southwestern University (SWU) PHINMA**. It connects an Android student application and a web-based administrative interface to a PHP backend and PostgreSQL database.

The system covers student accounts, scholarship information and applications, document submissions, service requests, notifications, and student support. The repository described here hosts the **PHP API**; the presence of the Android and React application source in this same repository has not been established.

## Project links

| Resource | Location |
| --- | --- |
| GitHub repository | https://github.com/javiaquilloswu-lab/SLS-web.3-vercel |
| Deployed API base URL | https://sls-web3-vercel.vercel.app/api |
| API entry-point / health check | `GET https://sls-web3-vercel.vercel.app/api` |
| API test collection | `postman/Student_Life_Services.postman_collection.json` **(add exported file before submission)** |

The deployed API and database connection were tested during development. Endpoint results and database contents may change; test the deployed service again before the final demonstration.

## Technology stack

| Component | Technology |
| --- | --- |
| API/backend | PHP (REST-style endpoints) |
| Deployment | Vercel, using a PHP API entry point |
| Database | PostgreSQL hosted on Supabase |
| Student client | Android (Java/XML) |
| Staff/admin client | React/TypeScript |
| API testing | Postman |

## Repository layout

```text
.
├── api/
│   └── index.php                    # Deployed API entry point / dispatcher
├── android_api/                    # PHP endpoints and shared modules
│   ├── auth.php
│   ├── db.php
│   ├── response.php
│   ├── login.php
│   ├── get_student.php
│   ├── scholarships.php
│   ├── get_service_types.php
│   ├── requests.php
│   ├── notifications.php
│   ├── chatbot.php
│   ├── admin_login.php
│   ├── admin_concerns.php
│   └── ...                         # Other student/admin PHP handlers
├── database/
│   └── student_life_schema.sql     # Exists locally; verify contents and commit
├── postman/
│   └── Student_Life_Services.postman_collection.json
│                                    # Export and add before submission
├── .env.example                    # Placeholder configuration variables
├── .gitignore
├── vercel.json                     # Vercel routing/build configuration
└── README.md
```

**Submission note:** The `database/` directory was untracked in the last Git status check, and the Postman export and `README.md` had not yet been verified as tracked. The layout above includes **intended submission files**, not a claim that all files are already published.

## Configuration

The PHP backend uses environment variables for database access. Copy the variable names from `.env.example` into your local runtime or Vercel project settings, replacing placeholders **outside version control**:

```dotenv
DB_HOST=your_database_host
DB_PORT=5432
DB_NAME=your_database_name
DB_USER=your_database_username
DB_PASSWORD=your_database_password
GEMINI_API_KEY=your_gemini_api_key
```

`GEMINI_API_KEY` is used by the chatbot when available; the chatbot PHP code also includes a fallback response path. Never commit a real `.env`, database password, Gemini API key, session token, or credential-bearing Postman environment.

**PHP requirements:** PHP with PDO PostgreSQL (`pdo_pgsql`) enabled; the chatbot's external Gemini call additionally depends on PHP cURL when that integration is used. The deployment currently includes `vercel.json`; no `composer.json` was found in the last local audit. Do not add a fabricated Composer dependency manifest. If external Composer packages are introduced, document and commit the real `composer.json`.

## Local setup

1. Clone the repository:

   ```bash
   git clone https://github.com/javiaquilloswu-lab/SLS-web.3-vercel.git
   cd SLS-web.3-vercel
   ```

2. Install PHP and enable the `pdo_pgsql` extension (and `curl` if testing the Gemini integration). XAMPP has been used during local development.
3. Configure the required environment variables in the PHP runtime. A `.env` file is **not automatically loaded by PHP** unless a loader is explicitly configured.
4. Connect to an authorized PostgreSQL database with the application's schema and appropriate demo records. Review `database/student_life_schema.sql` before using it; its completeness and seed records still need verification.
5. Start the API using the project's configured PHP/XAMPP web server and routing. **The exact local launch command and localhost route have not yet been verified**, so do not assume the Vercel URL layout is identical locally.
6. Test the API entry point and authenticated routes in Postman.

For a straightforward demonstration without local routing setup, use the deployed API URL above.

## Implemented database tables

The currently used PostgreSQL database has **13 tables**:

```text
students
student_tokens
admin_users
admin_tokens
scholarships
scholarship_applications
scholarship_requirements
service_types
student_requests
student_documents
student_notifications
student_concerns
student_concern_messages
```

The academic documentation also contains a **proposed 16-entity ERD**. That proposal is separate from the 13-table implementation and should **not** be described as an already applied migration.

## API endpoints and testing

The following routes and methods have been exercised or identified from the PHP source during integration testing. All paths below are relative to `https://sls-web3-vercel.vercel.app/api`.

| Method | Route | Purpose | Authentication | Verification status |
| --- | --- | --- | --- | --- |
| GET | `/` | API entry point / health check | None | Previously tested |
| POST | `/login` | Sign in as a student | None | Successful response confirmed |
| GET | `/get_student` | Retrieve authenticated student profile | Student token | Successful response confirmed |
| GET | `/scholarships` | Retrieve scholarship information | Student token used in tests | Successful response confirmed |
| GET | `/get_service_types` | List service types | Student token used in tests | Successful response confirmed; list was empty |
| GET | `/requests` | List student service requests | Student token | Successful response confirmed |
| POST | `/requests` | Create or cancel a service request via `action` | Student token | Create and cancel confirmed |
| GET | `/notifications` | List student notifications | Student token | Successful response confirmed |
| POST | `/notifications` | Update notification read status | Student token | Successful response confirmed |
| POST | `/admin_login` | Sign in as an administrator | None | Successful response reported |
| GET | `/admin_concerns` | List concerns or retrieve a ticket transcript | Admin token | Successful list response shown |
| POST | `/chatbot` | Request chatbot response; `create_concern` action submits a concern | Student token | Source code reviewed; persistence needs independent confirmation |
| POST | `/admin_concerns` | Staff takeover, reply, note, or status action | Admin token | Implemented in source; end-to-end testing not confirmed |

**Other modules exist**, including registration, document uploads, administrative scholarship and request handling, student management, and statistics. Their precise methods and request bodies must be checked against their PHP handlers before being added to a final Section 08 endpoint table. Map endpoints to the **actual UC numbers in the approved use-case document**; no UC numbers are assumed here.

### Postman collection setup

1. Open Postman and **Import** `postman/Student_Life_Services.postman_collection.json` after the file has been added to this repository.
2. Configure the collection variable `base_url` as `https://sls-web3-vercel.vercel.app/api`.
3. For student routes, first send `POST {{base_url}}/login` using a designated demo student account. Use the returned token as **Bearer Token** for subsequent student requests.
4. For admin routes, send `POST {{base_url}}/admin_login` using a designated demo administrator account. Use its **separate admin Bearer token**.
5. Use the saved request bodies in the collection and confirm HTTP status codes and response JSON. Creating service requests or concerns writes demo data to the database; avoid repeated submissions unless intended.
6. Before sharing or committing the collection, replace all saved passwords, tokens, and personal information with placeholders or variables. Do not export real credential values in a Postman environment.

**Instructor test access:** The course instructions request demo credentials when login is required. Supply active **limited-privilege test credentials through the instructor's approved private submission channel**, not in this public README. A `SUPER_ADMIN` demo account should not be left publicly accessible with a shared example password.

### Example request bodies

**Create a service request:** `POST /requests` with a student Bearer token:

```json
{
  "action": "create",
  "title": "Test Document Request",
  "department": "Office of Student Affairs",
  "category": "Document"
}
```

**Cancel a service request:** `POST /requests` with a student Bearer token and an existing request ID:

```json
{
  "action": "cancel",
  "request_id": 1
}
```

**Create a student concern:** `POST /chatbot` with a student Bearer token:

```json
{
  "action": "create_concern",
  "message": "I need assistance with my scholarship application."
}
```

**Caution:** The current chatbot handler can return a successful AI reply even when database logging fails. To verify concern creation, also check that the ticket appears in the authenticated admin concern list and, if necessary, in the database.

## Security and deployment checks

- Store secrets in Vercel environment settings or an appropriately protected local environment, never in the repository.
- Remove or restrict temporary database diagnostic/test endpoints before final public submission; previous Git listings included `db_test.php`, `test.connection.php`, `test_connection.php`, and `test_query_*.php`.
- Revoke any Bearer tokens exposed in screenshots or shared files.
- Verify authentication and authorization on protected student/admin routes.
- Check that SQL scripts contain no production secrets or real student personal information.
- Ensure the repository is public **or** the instructor has collaborator access, as required.
- Confirm that each group member has genuine commits in the history under their own Git identity.

## Project team

**TJF COMPANY — SWU PHINMA**

- Jash V. Aquillo
- Tynneth Jhoel Fuentes
- Faith A. Villamor

## Current status and remaining submission tasks

The PHP API has been deployed on Vercel and key authentication, profile, scholarship, service request, notification, and administrator routes have been tested. The remaining submission work is to:

- Verify the database SQL file includes the expected schema and safe sample data, then commit it.
- Add the sanitized Postman collection to the repository.
- Commit this README and verify the deployed GitHub branch contains it.
- Review temporary test endpoints and pending local changes before pushing.
- Verify the team's individual commit contributions.
- Complete the Section 08 endpoint-to-use-case mapping using the approved use-case documentation.

Do not mark these tasks complete until they have been verified in the GitHub repository.
