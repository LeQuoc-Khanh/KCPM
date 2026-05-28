"use client";

import { GoogleOAuthProvider } from "@react-oauth/google";

export default function GoogleOAuthWrapper({
  children,
}: {
  children: React.ReactNode;
}) {
  const clientId = "393439549456-uam90rsdkkr6bud5dglmto2gtcdejhp4.apps.googleusercontent.com";

  return (
    <GoogleOAuthProvider clientId={clientId}>
      {children}
    </GoogleOAuthProvider>
  );
}