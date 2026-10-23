import { createContext, useCallback, useContext, type Dispatch, type SetStateAction } from "react";
import { useLocation } from "react-router-dom";

export type PageBreadcrumb = { path: string; title: string };
export const PageBreadcrumbContext = createContext<Dispatch<SetStateAction<PageBreadcrumb | undefined>>>(() => undefined);

/** Replaces a generic nested breadcrumb with the name of the entity loaded by a detail page. */
export function usePageBreadcrumb() {
  const setBreadcrumb = useContext(PageBreadcrumbContext);
  const { pathname } = useLocation();
  return useCallback((title?: string) => {
    setBreadcrumb(title ? { path: pathname, title } : undefined);
  }, [pathname, setBreadcrumb]);
}
