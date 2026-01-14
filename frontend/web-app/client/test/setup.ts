import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

// Mock ResizeObserver
class ResizeObserverMock {
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
}

window.ResizeObserver = ResizeObserverMock;

// Mock PointerEvent
if (!window.PointerEvent) {
    (window as any).PointerEvent = class PointerEvent extends MouseEvent { };
}

// Automatically cleanup after each test
afterEach(() => {
    cleanup();
});
