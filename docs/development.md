# Local Development with Shared Libraries

Open CRM depends on two shared libraries that are maintained in separate repositories:

| Library | Used by | Repository |
|---------|---------|------------|
| `com.open-elements:spring-services` | Backend (Maven) | `open-elements/spring-services` |
| `@open-elements/ui` | Frontend (pnpm) | `open-elements/open-elements-ui` |

When developing features that require changes in a shared library, you can test against a local build before publishing a new release.

## Backend: `spring-services`

### 1. Build and install locally

In the `spring-services` repository:

```bash
cd ~/git/open-elements/spring-services
mvn install
```

This installs the current build into your local Maven repository (`~/.m2/repository`).

### 2. Use the SNAPSHOT version

In `open-crm/backend/pom.xml`, temporarily change the version to the SNAPSHOT version:

```xml
<dependency>
    <groupId>com.open-elements</groupId>
    <artifactId>spring-services</artifactId>
    <version>0.13.0-SNAPSHOT</version>
</dependency>
```

The version must match the version defined in the `spring-services` `pom.xml`.

### 3. Revert before committing

Change the version back to the released version before committing:

```xml
<version>0.12.0</version>
```

## Frontend: `@open-elements/ui`

### 1. Build the library locally

In the `open-elements-ui` repository:

```bash
cd ~/git/open-elements/open-elements-ui
pnpm install
pnpm build
```

### 2. Link globally

Still in the `open-elements-ui` directory:

```bash
pnpm link --global
```

This registers the local build as a globally available package.

### 3. Use the local version in Open CRM

In the `open-crm/frontend` directory:

```bash
cd ~/git/open-elements/open-crm/frontend
pnpm link --global @open-elements/ui
```

The frontend now uses your local `@open-elements/ui` build instead of the published npm version.

### 4. Unlink when done

```bash
cd ~/git/open-elements/open-crm/frontend
pnpm unlink @open-elements/ui
pnpm install
```

This restores the published version from the npm registry.

### Note

Unlike `mvn install`, `pnpm link` creates a symlink. Changes in the `open-elements-ui` build output are immediately visible without re-linking. However, you still need to run `pnpm build` in the UI repo after making changes.
