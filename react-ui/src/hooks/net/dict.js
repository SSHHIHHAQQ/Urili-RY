import { getDictValueEnum } from "@/services/system/dict";
export function useDictEnum(name) {
    const data = getDictValueEnum(name);
    return data;
}
