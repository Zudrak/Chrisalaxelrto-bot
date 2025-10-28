namespace Chrisalaxelrto.Bot.Services;

/// <summary>
/// A stream wrapper that allows pausing and resuming write operations to the underlying stream.
/// Useful for controlling audio playback in Discord voice channels.
/// </summary>
public class PausableStream : Stream
{
    private readonly Stream _innerStream;
    private volatile bool _isPaused;
    private readonly SemaphoreSlim _pauseSemaphore = new(1, 1);

    public PausableStream(Stream innerStream)
    {
        _innerStream = innerStream ?? throw new ArgumentNullException(nameof(innerStream));
    }

    public bool IsPaused
    {
        get => _isPaused;
        set => _isPaused = value;
    }

    public override bool CanRead => _innerStream.CanRead;
    public override bool CanSeek => _innerStream.CanSeek;
    public override bool CanWrite => _innerStream.CanWrite;
    public override long Length => _innerStream.Length;
    public override long Position
    {
        get => _innerStream.Position;
        set => _innerStream.Position = value;
    }

    public override void Flush() => _innerStream.Flush();
    public override Task FlushAsync(CancellationToken cancellationToken = default)
        => _innerStream.FlushAsync(cancellationToken);

    public override int Read(byte[] buffer, int offset, int count)
        => _innerStream.Read(buffer, offset, count);

    public override long Seek(long offset, SeekOrigin origin)
        => _innerStream.Seek(offset, origin);

    public override void SetLength(long value)
        => _innerStream.SetLength(value);

    public override void Write(byte[] buffer, int offset, int count)
    {
        WaitWhilePaused();
        _innerStream.Write(buffer, offset, count);
    }

    public override async Task WriteAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken)
    {
        await WaitWhilePausedAsync(cancellationToken);
        await _innerStream.WriteAsync(buffer, offset, count, cancellationToken);
    }

    public override async ValueTask WriteAsync(ReadOnlyMemory<byte> buffer, CancellationToken cancellationToken = default)
    {
        await WaitWhilePausedAsync(cancellationToken);
        await _innerStream.WriteAsync(buffer, cancellationToken);
    }

    private void WaitWhilePaused()
    {
        while (_isPaused)
        {
            Thread.Sleep(100); // Check every 100ms
        }
    }

    private async Task WaitWhilePausedAsync(CancellationToken cancellationToken)
    {
        while (_isPaused && !cancellationToken.IsCancellationRequested)
        {
            await Task.Delay(100, cancellationToken); // Check every 100ms
        }
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            _pauseSemaphore?.Dispose();
            _innerStream?.Dispose();
        }
        base.Dispose(disposing);
    }

    public override async ValueTask DisposeAsync()
    {
        _pauseSemaphore?.Dispose();
        if (_innerStream != null)
        {
            await _innerStream.DisposeAsync();
        }
        await base.DisposeAsync();
    }
}