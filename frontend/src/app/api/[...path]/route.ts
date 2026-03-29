import { auth } from "@/auth";
import { NextRequest } from "next/server";

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

async function handler(
  req: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const session = await auth();
  const { path } = await params;
  const target = `${backendUrl}/api/${path.join("/")}`;

  const url = new URL(target);
  const reqUrl = new URL(req.url);
  reqUrl.searchParams.forEach((value, key) => {
    url.searchParams.append(key, value);
  });

  const headers = new Headers();
  const contentType = req.headers.get("Content-Type");
  if (contentType) {
    headers.set("Content-Type", contentType);
  }
  const accept = req.headers.get("Accept");
  if (accept) {
    headers.set("Accept", accept);
  }
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }

  const hasBody = req.method !== "GET" && req.method !== "HEAD";

  const response = await fetch(url.toString(), {
    method: req.method,
    headers,
    body: hasBody ? await req.arrayBuffer() : undefined,
  });

  return new Response(response.body, {
    status: response.status,
    headers: response.headers,
  });
}

export { handler as GET, handler as POST, handler as PUT, handler as DELETE };
