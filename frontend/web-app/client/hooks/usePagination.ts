import { useState, useMemo } from "react";

export interface PaginationOptions {
  page: number;
  pageSize: number;
  totalItems: number;
}

export interface PaginationResult<T> {
  paginatedData: T[];
  page: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
  hasNextPage: boolean;
  hasPreviousPage: boolean;
  goToPage: (page: number) => void;
  nextPage: () => void;
  previousPage: () => void;
  setPageSize: (size: number) => void;
}

/**
 * Custom hook for client-side pagination
 * @param data - The full array of data to paginate
 * @param initialPageSize - Initial page size (default: 10)
 * @returns Pagination result with paginated data and controls
 */
export function usePagination<T>(
  data: T[],
  initialPageSize: number = 10
): PaginationResult<T> {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(initialPageSize);

  const totalPages = Math.ceil(data.length / pageSize);
  const totalItems = data.length;

  const paginatedData = useMemo(() => {
    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return data.slice(startIndex, endIndex);
  }, [data, page, pageSize]);

  const hasNextPage = page < totalPages;
  const hasPreviousPage = page > 1;

  const goToPage = (newPage: number) => {
    const pageNumber = Math.max(1, Math.min(newPage, totalPages));
    setPage(pageNumber);
  };

  const nextPage = () => {
    if (hasNextPage) {
      setPage((prev) => prev + 1);
    }
  };

  const previousPage = () => {
    if (hasPreviousPage) {
      setPage((prev) => prev - 1);
    }
  };

  const handleSetPageSize = (size: number) => {
    setPageSize(size);
    // Reset to page 1 when changing page size
    setPage(1);
  };

  return {
    paginatedData,
    page,
    pageSize,
    totalPages,
    totalItems,
    hasNextPage,
    hasPreviousPage,
    goToPage,
    nextPage,
    previousPage,
    setPageSize: handleSetPageSize,
  };
}
