import "@testing-library/jest-dom";

// Radix UI primitives (Select, Dialog, Tooltip, etc.) call matchMedia during
// mount; jsdom doesn't implement it, so every test touching those components
// would otherwise throw "matchMedia is not a function".
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: jest.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Radix UI's ScrollArea/Select also probe ResizeObserver, which jsdom lacks.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = window.ResizeObserver ?? (ResizeObserverStub as unknown as typeof ResizeObserver);

// jsdom doesn't implement scrollIntoView (used e.g. by the chatbot page to
// keep the latest message in view) — stub it the same way as the two above.
window.HTMLElement.prototype.scrollIntoView = window.HTMLElement.prototype.scrollIntoView ?? jest.fn();
