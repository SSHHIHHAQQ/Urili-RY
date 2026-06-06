export function getValueEnumLabel(options, val, defaultValue) {
    if (val !== undefined) {
        const data = options[val];
        if (data) {
            return data.text;
        }
    }
    return defaultValue ? defaultValue : val;
}
