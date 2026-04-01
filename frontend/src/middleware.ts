export { auth as middleware } from "@/auth";

export const config = {
  matcher: ["/((?!api/auth|api/logout|login|_next/static|_next/image|favicon\\.ico|.*\\.svg$|.*\\.png$|.*\\.jpg$|.*\\.ico$).*)"],
};
