namespace CnHolidayKit;

internal sealed class LruCache<TKey, TValue>
    where TKey : notnull
    where TValue : class
{
    private readonly int _capacity;
    private readonly Dictionary<TKey, LinkedListNode<CacheEntry>> _entries = new();
    private readonly LinkedList<CacheEntry> _accessOrder = new();
    private readonly object _syncRoot = new();

    public LruCache(int capacity = 32)
    {
        if (capacity <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(capacity), $"capacity 必须为正数: {capacity}");
        }

        _capacity = capacity;
    }

    public TValue? Get(TKey key)
    {
        lock (_syncRoot)
        {
            if (!_entries.TryGetValue(key, out var node))
            {
                return null;
            }

            Touch(node);
            return node.Value.Value;
        }
    }

    public TValue? GetOrAdd(TKey key, Func<TKey, TValue?> valueFactory)
    {
        lock (_syncRoot)
        {
            if (_entries.TryGetValue(key, out var existing))
            {
                Touch(existing);
                return existing.Value.Value;
            }
        }

        var created = valueFactory(key);
        if (created is null)
        {
            return null;
        }

        lock (_syncRoot)
        {
            if (_entries.TryGetValue(key, out var existing))
            {
                Touch(existing);
                return existing.Value.Value;
            }

            var node = new LinkedListNode<CacheEntry>(new CacheEntry(key, created));
            _accessOrder.AddLast(node);
            _entries[key] = node;
            EvictIfNeeded();
            return created;
        }
    }

    private void Touch(LinkedListNode<CacheEntry> node)
    {
        if (ReferenceEquals(_accessOrder.Last, node))
        {
            return;
        }

        _accessOrder.Remove(node);
        _accessOrder.AddLast(node);
    }

    private void EvictIfNeeded()
    {
        while (_entries.Count > _capacity)
        {
            var oldest = _accessOrder.First;
            if (oldest is null)
            {
                return;
            }

            _accessOrder.RemoveFirst();
            _entries.Remove(oldest.Value.Key);
        }
    }

    private sealed class CacheEntry
    {
        public CacheEntry(TKey key, TValue value)
        {
            Key = key;
            Value = value;
        }

        public TKey Key { get; }

        public TValue Value { get; }
    }
}
