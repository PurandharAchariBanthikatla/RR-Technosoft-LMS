import { PaymentOrder } from "@/types";

declare global {
  interface Window {
    Razorpay: new (options: RazorpayOptions) => { open: () => void };
  }
}

interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description?: string;
  order_id: string;
  prefill?: { name?: string; email?: string; contact?: string };
  theme?: { color?: string };
  handler: (response: RazorpayCheckoutResponse) => void;
  modal?: { ondismiss?: () => void };
}

export interface RazorpayCheckoutResponse {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}

let scriptPromise: Promise<void> | null = null;

/** Lazily loads the Razorpay Checkout.js SDK exactly once. */
function loadRazorpayScript(): Promise<void> {
  if (typeof window === "undefined") return Promise.reject(new Error("Not in browser"));
  if (window.Razorpay) return Promise.resolve();
  if (scriptPromise) return scriptPromise;

  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      scriptPromise = null;
      reject(new Error("Could not load the Razorpay checkout script. Check your connection and try again."));
    };
    document.body.appendChild(script);
  });
  return scriptPromise;
}

/**
 * Opens the Razorpay checkout modal for a payment order returned by
 * POST /finance/payments/initiate, and resolves with the raw gateway
 * response once the user completes payment. The caller is responsible for
 * posting that response to POST /finance/payments/verify — this helper only
 * drives the widget.
 */
export async function openRazorpayCheckout(
  order: PaymentOrder,
  opts: { studentName?: string; studentEmail?: string; description?: string }
): Promise<RazorpayCheckoutResponse> {
  await loadRazorpayScript();

  return new Promise((resolve, reject) => {
    const rzp = new window.Razorpay({
      key: order.keyId,
      amount: Math.round(order.amount * 100), // paise
      currency: order.currency,
      name: "RR TECHNOSOFT",
      description: opts.description ?? "Course fee payment",
      order_id: order.gatewayOrderId,
      prefill: { name: opts.studentName, email: opts.studentEmail },
      theme: { color: "#4f46e5" },
      handler: (response) => resolve(response),
      modal: {
        ondismiss: () => reject(new Error("Payment window closed before completion.")),
      },
    });
    rzp.open();
  });
}
